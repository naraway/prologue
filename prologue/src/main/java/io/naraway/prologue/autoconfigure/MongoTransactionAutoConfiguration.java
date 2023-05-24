/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
*/

package io.naraway.prologue.autoconfigure;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoConfiguration
@ConditionalOnMissingClass({
        "org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
        "org.spockframework.runtime.PlatformSpecRunner"})
@ConditionalOnClass(MongoClient.class)
@ConditionalOnProperty(
        name = "nara.prologue.data.mongodb.transaction.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MongoTransactionAutoConfiguration extends AbstractMongoClientConfiguration {
    //
    @Value("${spring.data.mongodb.uri:}")
    private String uri;
    @Value("${spring.data.mongodb.host:}")
    private String host;
    @Value("${spring.data.mongodb.port:27017}")
    private int port;
    @Value("${spring.data.mongodb.authentication-database:}")
    private String authenticationDatabase;
    @Value("${spring.data.mongodb.database:}")
    private String database;
    @Value("${spring.data.mongodb.username:}")
    private String username;
    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Bean
    @ConditionalOnMissingBean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        //
        return new MongoTransactionManager(dbFactory);
    }

    @Override
    protected String getDatabaseName() {
        //
        if (StringUtils.hasText(this.uri)) {
            return new ConnectionString(this.uri).getDatabase();
        }

        return this.database;
    }

    @Override
    public MongoClient mongoClient() {
        //
        if (StringUtils.hasText(this.uri)) {
            return getUriMongoClient();
        }

        return getCredentialMongoClient();
    }

    private MongoClient getUriMongoClient() {
        //
        ConnectionString connectionString = new ConnectionString(this.uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(settings);
    }

    private MongoClient getCredentialMongoClient() {
        //
        MongoCredential credential = MongoCredential.createCredential(
                this.username,
                this.authenticationDatabase,
                this.password.toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .retryWrites(true)
                .applyToConnectionPoolSettings(builder -> builder.maxConnectionIdleTime(
                        5000, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder -> builder.hosts(List.of(
                        new ServerAddress(this.host, this.port))))
                .build();

        return MongoClients.create(settings);
    }
}
