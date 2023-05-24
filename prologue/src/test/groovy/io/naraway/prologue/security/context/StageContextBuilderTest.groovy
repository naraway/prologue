package io.naraway.prologue.security.context

import io.naraway.accent.domain.context.StageContext
import io.naraway.accent.domain.context.StageRequest
import io.naraway.accent.domain.context.UserType
import io.naraway.accent.domain.tenant.ActorKey
import io.naraway.prologue.autoconfigure.PrologueProperties
import io.naraway.prologue.security.auth.jwt.JwtSupport
import org.springframework.http.HttpHeaders
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

import javax.servlet.http.HttpServletRequest

class StageContextBuilderTest extends Specification {
    //
    @Subject
    StageContextBuilder stageRequestBuilder

    //
    def setup() {
        def jwtSupport = new JwtSupport(new PrologueProperties())
        stageRequestBuilder = new StageContextBuilder(jwtSupport)
    }

    //
    @Ignore('token signature cannot be verified cause different secret key')
    def 'build request from citizen request'() {
        given:
        def actorId = ActorKey.sample().id
        def headers = [:]
        headers[StageContextBuilder.ROLES] = StageRequest.sample().roles.join(',')
        headers[StageContextBuilder.ACTOR_ID] = actorId
        headers[HttpHeaders.AUTHORIZATION] = 'Bearer ' + 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJOYXJhIFdheSIsImF1dGhvcml0aWVzIjoiIiwic2NvcGUiOlsiaW50ZXJuYWwiXSwidXNlcm5hbWUiOiJuYXJhd2F5QG5leHRyZWUuaW8iLCJkaXNwbGF5TmFtZSI6Ik5hcmEgV2F5IiwiY2luZXJvb21JZHMiOlsiMToxOjE6MSIsIjE6MToxOjIiXSwidXNlclR5cGUiOiJpbnRlcm5hbCIsImp0aSI6IjktZldzS0Y3QW5YYi5vRGdYbVNSLWwiLCJpYXQiOjE2NzMyNTU2NjksImV4cCI6MTY3MzM0MjA2OX0.hZVQu34uAgUKVKdMUCXR9m1YbPyibRqU8rXHHuPkpY2H0uzFc6hBLc4mCnUZnY9QAWQcqvWC7oxH4sIktbspbg'
        def request = Mock(HttpServletRequest)
        request.getHeaderNames() >> Collections.enumeration(headers.keySet())
        request.getHeader(StageContextBuilder.ROLES) >> headers.get(StageContextBuilder.ROLES)
        request.getHeader(StageContextBuilder.ACTOR_ID) >> headers.get(StageContextBuilder.ACTOR_ID)
        request.getHeader(HttpHeaders.AUTHORIZATION) >> headers.get(HttpHeaders.AUTHORIZATION)

        when:
        stageRequestBuilder.buildRequest(request)

        then:
        def stageRequest = StageContext.get();
        println(stageRequest)
        stageRequest != null
        stageRequest.actorId == actorId
    }

    //
    @Ignore('token signature cannot be verified cause different secret key')
    def 'build request from internal request'() {
        given:
        def headers = [:]
        headers[HttpHeaders.AUTHORIZATION] = 'Bearer ' + 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJkaXNwbGF5TmFtZSI6Ik5hcmEgV2F5Iiwic2NvcGUiOlsiaW50ZXJuYWwiXSwidXNlclR5cGUiOiJpbnRlcm5hbCIsImV4cCI6MTY3NDAzNDU3MywiaWF0IjoxNjczOTQ4MTczLCJhdXRob3JpdGllcyI6IiIsImNpbmVyb29tSWRzIjpbIjE6MToxOjEiLCIxOjE6MToyIl0sImp0aSI6ImRraFBuaE05UTNzOEJJRGpJSXJHSUsiLCJ1c2VybmFtZSI6Im5hcmF3YXlAbmV4dHJlZS5pbyJ9.zNHPVubk6JS_h9NOD9_p46DFR_3Y3yvo6pr2xnLdsuIQ2FQq3fa0UIvchFpdO0C-hVp8zgudlELG3DHj25qYzQ'
        def request = Mock(HttpServletRequest)
        request.getHeaderNames() >> Collections.enumeration(headers.keySet())
        request.getHeader(HttpHeaders.AUTHORIZATION) >> headers.get(HttpHeaders.AUTHORIZATION)

        when:
        stageRequestBuilder.buildRequest(request)

        then:
        def dramaRequest = StageContext.get();
        println(dramaRequest)
        dramaRequest != null
        dramaRequest.userType == UserType.Internal
    }
}
