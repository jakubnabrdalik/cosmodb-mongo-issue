package com.vattenfall.reactivemongodb;


import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

interface ReactiveRepo extends Repository<DomainObject, String> {
    Mono<DomainObject> insert(DomainObject entity);
}


