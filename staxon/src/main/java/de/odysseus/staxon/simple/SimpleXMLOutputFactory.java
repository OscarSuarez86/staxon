package de.odysseus.staxon.simple;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

public class SimpleXMLOutputFactory extends XMLOutputFactory {
	@Override
	public XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException {
		return new SimpleXMLStreamWriter(stream);
	}

	@Override
	public XMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
		return createXMLStreamWriter(stream, "UTF-8");
	}

	@Override
	public XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException {
		try {
			return new SimpleXMLStreamWriter(new OutputStreamWriter(stream, encoding));
		} catch (UnsupportedEncodingException e) {
			throw new XMLStreamException(e);
		}
	}

	@Override
	public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProperty(String name, Object value) throws IllegalArgumentException {
		if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
			if (Boolean.valueOf(value.toString())) {
				throw new IllegalArgumentException();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
			return false;
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean isPropertySupported(String name) {
		if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
			return true;
		} else {
			return false;
		}
	}
}
