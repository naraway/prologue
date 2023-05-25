/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.prologue.shared.dock;

import com.fasterxml.jackson.core.type.TypeReference;
import io.naraway.accent.domain.message.QueryRequest;
import io.naraway.accent.domain.message.QueryResponse;
import io.naraway.accent.domain.tenant.CitizenKey;
import io.naraway.prologue.autoconfigure.PrologueProperties;
import io.naraway.prologue.shared.auth.InternalAuthProvider;
import io.naraway.prologue.support.rest.RestRequester;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.NoSuchElementException;

@Slf4j
@RequiredArgsConstructor
public class DockProxy {
    //
    private final PrologueProperties properties;
    private final InternalAuthProvider internalAuthProvider;

    private RestRequester rest;

    @PostConstruct
    private void initialize() {
        //
        this.rest = new RestRequester(this.properties.getDock().getRest(), this.internalAuthProvider);
    }

    // prevent exposure other package
    Dock findDock(String citizenId) {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();

        if (this.rest == null) {
            return null;
        }

        if (props.getRest().isLoopback()) {
            return Dock.sample();
        }

        FindSessionDockQuery query = new FindSessionDockQuery(citizenId);
        QueryResponse<Dock> response = this.rest.post(
                "/feature/dock/find-session-dock/query", query,
                new TypeReference<QueryResponse<Dock>>() {}).block();

        if (response == null) {
            throw new NoSuchElementException("citizenId = " + citizenId);
        }

        Dock dock = response.getQueryResult();

        if (dock == null) {
            throw new NoSuchElementException("citizenId = " + citizenId);
        }

        return dock;
    }

    // prevent exposure other package
    String findOsid(String citizenId) {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();

        if (this.rest == null) {
            return null;
        }

        if (props.getRest().isLoopback()) {
            return "1@1:1:1";
        }

        String path = UriComponentsBuilder
                .fromUriString("/restful/pavilions/{id}")
                .buildAndExpand(CitizenKey.fromId(citizenId).genPavilionId())
                .toUriString();

        Pavilion pavilion = rest.get(path, Pavilion.class).block();

        if (pavilion == null) {
            throw new NoSuchElementException("citizenId = " + citizenId);
        }

        return pavilion.getOsid();
    }

    // prevent exposure other package
    String findUsid(String citizenId) {
        //
        PrologueProperties.DockProperties props = this.properties.getDock();

        if (this.rest == null) {
            return null;
        }

        if (props.getRest().isLoopback()) {
            return "1@1:1:1";
        }

        String path = UriComponentsBuilder
                .fromUriString("/restful/citizens/{citizen-id}")
                .buildAndExpand(citizenId)
                .toUriString();

        Citizen citizen = rest.get(path, Citizen.class).block();

        if (citizen == null) {
            throw new NoSuchElementException("citizenId = " + citizenId);
        }

        return citizen.getUsid();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FindSessionDockQuery extends QueryRequest<Dock> {
        //
        private String citizenId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Pavilion {
        //
        private String id;
        private String osid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Citizen {
        //
        private String id;
        private String usid;
    }
}
