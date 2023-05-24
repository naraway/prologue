package io.naraway.prologue.shared.filter.xss.converter

import io.naraway.prologue.security.web.xss.converter.XssHtmlEscapeConverter
import io.naraway.prologue.security.web.xss.converter.XssHtmlUnescape
import spock.lang.Specification
import spock.lang.Subject

class XssHtmlEscapeConverterTest extends Specification {
    //
    @Subject
    XssHtmlEscapeConverter xssHtmlEscapeConverter

    //
    def 'convert'() {
        given:
        def html = '<html lang="en"><p style=".base">&rt; Test</p></html>'

        when:
        def escaped = new XssHtmlEscapeConverter().convert(html)

        then:
        println(escaped)
        escaped == '&lt;html lang=&quot;en&quot;&gt;&lt;p style=&quot;.base&quot;&gt;&amp;rt; Test&lt;/p&gt;&lt;/html&gt;'
        html == XssHtmlUnescape.unescape(escaped)
    }
}
