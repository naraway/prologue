package io.naraway.prologue.shared.filter.xss.converter

import io.naraway.prologue.security.web.xss.converter.XssJsonEscapeConverter
import io.naraway.prologue.security.web.xss.converter.XssJsonUnescape
import spock.lang.Specification
import spock.lang.Subject

class XssJsonEscapeConverterTest extends Specification {
    //
    @Subject
    XssJsonEscapeConverter xssJsonEscapeConverter

    //
    def 'convert'() {
        given:
        def json = '{ "name": ["Steve", "Jobs"], "age": 45, "active": false, "content": "<p>You&Me &amp; link\'s url</p>" }';

        when:
        def escaped = new XssJsonEscapeConverter().convert(json)

        then:
        println(escaped)
        escaped == '{ "name": ["Steve", "Jobs"], "age": 45, "active": false, "content": "&lt;p&gt;You&Me &amp; link\'s url&lt;/p&gt;" }'
        json == XssJsonUnescape.unescape(escaped)
    }
}
