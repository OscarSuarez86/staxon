package de.odysseus.staxon;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import de.odysseus.staxon.simple.SimpleXMLStreamWriter;
import de.odysseus.staxon.util.StreamWriterDelegate;

public class AbstractXMLStreamWriterTest {
	boolean jdkStreamWriter = false;
	
	XMLStreamWriter createXMLStreamWriter() throws XMLStreamException {
		final StringWriter writer = new StringWriter();
		if (jdkStreamWriter) {
			return new StreamWriterDelegate(XMLOutputFactory.newFactory().createXMLStreamWriter(writer)) {
				@Override
				public String toString() {
					return writer.toString();
				}
			};
		} else {
			return new SimpleXMLStreamWriter(writer) {
				@Override
				public String toString() {
					return writer.toString();
				}
			};
		}
	}
	
	@Test
	public void testWriteAttribute0() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeAttribute("bar", "foobar");
		writer.flush();
		Assert.assertEquals("<foo bar=\"foobar\"", writer.toString());
	}

	@Test
	@Ignore
	public void testWriteAttribute0a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeAttribute("bar", "<>'\"&");
		writer.flush();
		Assert.assertEquals("<foo bar=\"&lt;&gt;'&quot;&amp;\"", writer.toString());
	}

	@Test
	public void testWriteAttribute1a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("foo");
		writer.writeAttribute("http://p", "bar", "foobar");
		writer.flush();
		Assert.assertEquals("<foo p:bar=\"foobar\"", writer.toString());
	}

	@Test(expected = XMLStreamException.class)
	public void testWriteAttribute1b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeAttribute("http://p", "bar", "foobar");
	}

	@Test
	public void testWriteAttribute2a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("foo");
		writer.writeAttribute("p", "http://p", "bar", "foobar");
		writer.flush();
		Assert.assertEquals("<foo p:bar=\"foobar\"", writer.toString());
	}

	@Test
	public void testWriteAttribute2b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeAttribute("p", "http://p", "bar", "foobar");
		writer.flush();
//		Assert.assertEquals("<foo xmlns:p=\"http://p\" p:bar=\"foobar\"", writer.toString()); // according to XMLStreamWriter javadoc
		Assert.assertEquals("<foo p:bar=\"foobar\"", writer.toString()); // according to implementations
	}

	@Test
	public void testWriteAttribute2c() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("foo");
		writer.writeAttribute("pp", "http://p", "bar", "foobar");
//		Assert.fail("expected exception: bound to another prefix"); // according to XMLStreamWriter javadoc
		writer.flush();
		Assert.assertEquals("<foo pp:bar=\"foobar\"", writer.toString()); // according to implementations
	}

	@Test
	public void testWriteElement0() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo></foo>", writer.toString());
	}

	@Test
	public void testWriteElement1a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("http://p", "foo");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<p:foo></p:foo>", writer.toString());
	}

	@Test(expected = XMLStreamException.class)
	public void testWriteElement1b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("http://p", "foo");
	}

	@Test
	public void testWriteElement2a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("p", "foo", "http://p");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<p:foo></p:foo>", writer.toString());
	}

	@Test
	public void testWriteElement2b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeStartElement("pp", "foo", "http://p");
//		Assert.fail("expected exception: bound to another prefix"); // according to XMLStreamWriter javadoc
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<pp:foo></pp:foo>", writer.toString()); // according to implementations
	}

	@Test
	public void testWriteElement2c() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("p", "foo", "http://p");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<p:foo></p:foo>", writer.toString());
	}

	@Test
	public void testWriteElementAddsPrefixBinding() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("p", "foo", "http://p");
		Assert.assertEquals("p", writer.getPrefix("http://p"));
		Assert.assertEquals("http://p", writer.getNamespaceContext().getNamespaceURI("p"));
		Assert.assertEquals("p", writer.getNamespaceContext().getPrefix("http://p"));
	}

	@Test
	public void testWriteEmptyElement0() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeEmptyElement("foo");
		writer.flush();
		Assert.assertEquals("<foo", writer.toString());
	}

	@Test
	public void testWriteEmptyElement1a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeEmptyElement("http://p", "foo");
		writer.flush();
		Assert.assertEquals("<p:foo", writer.toString());
	}

	@Test(expected = XMLStreamException.class)
	public void testWriteEmptyElement1b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("http://p", "foo");
	}

	@Test
	public void testWriteEmptyElement2a() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeEmptyElement("p", "foo", "http://p");
		writer.flush();
		Assert.assertEquals("<p:foo", writer.toString());
	}

	@Test
	public void testWriteEmptyElement2b() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.setPrefix("p", "http://p");
		writer.writeEmptyElement("pp", "foo", "http://p");
//		Assert.fail("expected exception: bound to another prefix"); // according to XMLStreamWriter javadoc
		writer.flush();
		Assert.assertEquals("<pp:foo", writer.toString()); // according to implementations
	}

	@Test
	public void testWriteEmptyElement2c() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeEmptyElement("p", "foo", "http://p");
		writer.flush();
		Assert.assertEquals("<p:foo", writer.toString());
	}

	@Test
	public void testWriteElementMultipleRoots() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeEndElement();
		writer.writeStartElement("foo");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo></foo><foo></foo>", writer.toString());
	}

	@Test(expected = XMLStreamException.class)
	public void testWriteElementMultipleRoots2() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartDocument();
		writer.writeStartElement("foo");
		writer.writeEndElement();
		writer.writeStartElement("foo");
	}

	@Test
	public void testWriterCharacters() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeCharacters("bar");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo>bar</foo>", writer.toString());
	}

	@Test
	@Ignore
	public void testWriterCharacters2() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeCharacters("<>'\"&");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo>&lt;&gt;'\"&amp;</foo>", writer.toString());
	}

	@Test
	public void testWriteCDtata() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeCData("bar");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo><![CDATA[bar]]></foo>", writer.toString());
	}

	@Test
	public void testWriteCDtata2() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeCData("<>'\"&");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo><![CDATA[<>'\"&]]></foo>", writer.toString());
	}

	@Test
	public void testWriteComment() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeComment("bar");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo><!--bar--></foo>", writer.toString());
	}


	@Test
	public void testWriteEntityRef() throws XMLStreamException {
		XMLStreamWriter writer = createXMLStreamWriter();
		writer.writeStartElement("foo");
		writer.writeEntityRef("bar");
		writer.writeEndElement();
		writer.flush();
		Assert.assertEquals("<foo>&bar;</foo>", writer.toString());
	}
}
