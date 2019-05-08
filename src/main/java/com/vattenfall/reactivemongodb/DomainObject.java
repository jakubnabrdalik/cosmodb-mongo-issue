package com.vattenfall.reactivemongodb;

import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = DomainObject.collectionName)
@AllArgsConstructor
class DomainObject {
    static final String collectionName = "TestObject";

    @Id
    String id;

    SubObject so1;
    SubObject so2;
    SubObject so3;
    SubObject so4;
    SubObject so5;

    static DomainObject create() {
        String id = UUID.randomUUID().toString();
        return new DomainObject(id, SubObject.create(), SubObject.create(), SubObject.create(), SubObject.create(), SubObject.create());
    }
}


@AllArgsConstructor
class SubObject {
    String f1;
    String f2;
    String f3;
    String f4;
    String f5;
    String f6;
    String f7;
    String f8;
    String f9;
    String f10;

    static SubObject create() {
        String r = UUID.randomUUID().toString();
        return new SubObject(r, r, r, r, r, r, r, r, r, r);
    }
}
