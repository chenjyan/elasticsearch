/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.mapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.lucene.search.Queries;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.test.VersionUtils;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class TypeFieldTypeTests extends FieldTypeTestCase {
    @Override
    protected MappedFieldType createDefaultFieldType() {
        return new TypeFieldMapper.TypeFieldType();
    }

    public void testTermsQuery() throws Exception {
        QueryShardContext context = Mockito.mock(QueryShardContext.class);
        Version indexVersionCreated = VersionUtils.randomVersionBetween(random(), Version.V_6_0_0, Version.CURRENT);
        Settings indexSettings = Settings.builder()
                .put(IndexMetaData.SETTING_VERSION_CREATED, indexVersionCreated)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_INDEX_UUID, UUIDs.randomBase64UUID()).build();
        IndexMetaData indexMetaData = IndexMetaData.builder(IndexMetaData.INDEX_UUID_NA_VALUE).settings(indexSettings).build();
        IndexSettings mockSettings = new IndexSettings(indexMetaData, Settings.EMPTY);
        Mockito.when(context.getIndexSettings()).thenReturn(mockSettings);
        Mockito.when(context.indexVersionCreated()).thenReturn(indexVersionCreated);

        MapperService mapperService = Mockito.mock(MapperService.class);
        Set<String> types = Collections.emptySet();
        Mockito.when(mapperService.types()).thenReturn(types);
        Mockito.when(context.getMapperService()).thenReturn(mapperService);

        TypeFieldMapper.TypeFieldType ft = new TypeFieldMapper.TypeFieldType();
        ft.setName(TypeFieldMapper.NAME);
        Query query = ft.termQuery("my_type", context);
        assertEquals(new MatchNoDocsQuery(), query);

        types = Collections.singleton("my_type");
        Mockito.when(mapperService.types()).thenReturn(types);
        query = ft.termQuery("my_type", context);
        assertEquals(new MatchAllDocsQuery(), query);

        Mockito.when(mapperService.hasNested()).thenReturn(true);
        query = ft.termQuery("my_type", context);
        assertEquals(Queries.newNonNestedFilter(context.indexVersionCreated()), query);

        types = Collections.singleton("other_type");
        Mockito.when(mapperService.types()).thenReturn(types);
        query = ft.termQuery("my_type", context);
        assertEquals(new MatchNoDocsQuery(), query);
    }


    static DirectoryReader openReaderWithNewType(String type, IndexWriter writer) throws IOException {
        Document doc = new Document();
        StringField typeField = new StringField(TypeFieldMapper.NAME, type, Store.NO);
        doc.add(typeField);
        writer.addDocument(doc);
        return DirectoryReader.open(writer);
    }
}
