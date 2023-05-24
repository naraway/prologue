package io.naraway.prologue.shared.filter.xss.converter

import io.naraway.prologue.security.web.xss.converter.XssRemoveConverter
import spock.lang.Specification
import spock.lang.Subject

class XssRemoveConverterTest extends Specification {
    //
    @Subject
    XssRemoveConverter xssRemoveConverter

    //
    def 'convert'() {
        given:
        def html = '<html lang="en"><p style=".base">&rt; Test</p></html>';

        when:
        def removed = new XssRemoveConverter().convert(html)

        then:
        println(removed)
        removed == '&rt; Test'
    }
}
