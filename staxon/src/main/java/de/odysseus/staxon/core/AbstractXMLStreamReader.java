package de.odysseus.staxon.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Abstract XML stream reader.
 */
public abstract class AbstractXMLStreamReader<T> implements XMLStreamReader {
	class Event {
		private final int type;
		private final XMLStreamReaderScope<T> scope;
		private final String text;

		Event(int type, XMLStreamReaderScope<T> scope, String text) {
			this.type = type;
			this.scope = scope;
			this.text = text;
		}

		XMLStreamReaderScope<T> getScope() {
			return scope;
		}

		int getType() {
			return type;
		}

		String getText() {
			return text;
		}

		@Override
		public String toString() {
			return new StringBuilder()
			.append(getEventName(type))
			.append(": ").append(getText() != null ? getText() : getScope().getTagName())
			.toString();
		}
	}

	static String getEventName(int type) {
		switch (type) {
		case XMLStreamConstants.ATTRIBUTE: return "ATTRIBUTE";
		case XMLStreamConstants.CDATA: return "CDATA";
		case XMLStreamConstants.CHARACTERS: return "CHARACTERS";
		case XMLStreamConstants.COMMENT: return "COMMENT";
		case XMLStreamConstants.DTD: return "DTD";
		case XMLStreamConstants.END_DOCUMENT: return "END_DOCUMENT";
		case XMLStreamConstants.END_ELEMENT: return "END_ELEMENT";
		case XMLStreamConstants.ENTITY_DECLARATION: return "ENTITY_DECLARATION";
		case XMLStreamConstants.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
		case XMLStreamConstants.NAMESPACE: return "NAMESPACE";
		case XMLStreamConstants.NOTATION_DECLARATION: return "NOTATION_DECLARATION";
		case XMLStreamConstants.PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
		case XMLStreamConstants.SPACE: return "SPACE";
		case XMLStreamConstants.START_DOCUMENT: return "START_DOCUMENT";
		case XMLStreamConstants.START_ELEMENT: return "START_ELEMENT";
		default: return String.valueOf(type); // should not happen...
		}
	}

	private final Queue<Event> queue = new LinkedList<Event>();

	private XMLStreamReaderScope<T> scope;
	private boolean moreTokens;
	private Event event;
	private List<Pair<String, String>> pendingAttributes = new ArrayList<Pair<String,String>>(16);
	
	public AbstractXMLStreamReader(T rootInfo) {
		scope = new XMLStreamReaderScope<T>(XMLConstants.NULL_NS_URI, rootInfo);
	}
	
	private void ensureStartTagClosed() throws XMLStreamException {
		if (!scope.isStartTagClosed()) {
			if (!pendingAttributes.isEmpty()) {
				for (Pair<String, String> attribute : pendingAttributes) {
					String name = attribute.getFirst();
					int colon = name.indexOf(':');
					String prefix = name.substring(0, colon);
					String namespaceURI = scope.getNamespaceURI(prefix);
					if (namespaceURI == null || XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
						throw new XMLStreamException("Unbound attribute prefix: " + prefix);
					}
					QName qName = new QName(namespaceURI, name.substring(colon + 1), prefix);
					scope.addAttribute(qName, attribute.getSecond());
				}
				pendingAttributes.clear();
			}
			scope.setStartTagClosed(true);
		}
	}
	
	protected void readStartDocument() {
		queue.add(new Event(XMLStreamConstants.START_DOCUMENT, scope, null));
	}
	
	protected XMLStreamReaderScope<T> readStartElementTag(String name) throws XMLStreamException {
		ensureStartTagClosed();
		int colon = name.indexOf(':');
		if (colon < 0) {
			scope = new XMLStreamReaderScope<T>(scope, XMLConstants.DEFAULT_NS_PREFIX, name);
		} else {
			scope = new XMLStreamReaderScope<T>(scope, name.substring(0, colon), name.substring(colon + 1));
		}
		queue.add(new Event(XMLStreamConstants.START_ELEMENT, scope, null));
		return scope;
	}
	
	protected void readProperty(String name, String value) throws XMLStreamException {
		if (XMLConstants.XMLNS_ATTRIBUTE.equals(name)) {
			scope.addNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX, value);
		} else if (name.startsWith(XMLConstants.XMLNS_ATTRIBUTE)
				&& name.charAt(XMLConstants.XMLNS_ATTRIBUTE.length()) == ':') {
			scope.addNamespaceURI(name.substring(XMLConstants.XMLNS_ATTRIBUTE.length() + 1), value);
		} else { // normal attribute
			int colon = name.indexOf(':');
			if (colon < 0) {
				scope.addAttribute(new QName(name), value);
			} else {
				String prefix = name.substring(0, colon);
				String namespaceURI = scope.getNamespaceURI(prefix);
				if (namespaceURI == null || XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
					pendingAttributes.add(new Pair<String, String>(name, value));
				} else {
					QName qName = new QName(namespaceURI, name.substring(colon + 1), prefix);
					scope.addAttribute(qName, value);
				}
			}
		}		
	}

	protected void readData(String text, int type) throws XMLStreamException {
		switch (type) {
		case XMLStreamConstants.CHARACTERS:
		case XMLStreamConstants.CDATA:
		case XMLStreamConstants.SPACE:
		case XMLStreamConstants.ENTITY_REFERENCE:
		case XMLStreamConstants.COMMENT:
			ensureStartTagClosed();
			queue.add(new Event(type, scope, text));
			break;
		default:
			throw new XMLStreamException("Unexpected event type " + getEventName(), getLocation());
		}
	}

	protected void readPI(String target, String data) throws XMLStreamException {
		ensureStartTagClosed();
		String text = data == null ? target : target + ':' + data;
		queue.add(new Event(XMLStreamConstants.PROCESSING_INSTRUCTION, scope, text));
	}

	protected void readEndElementTag() throws XMLStreamException {
		ensureStartTagClosed();
		queue.add(new Event(XMLStreamConstants.END_ELEMENT, scope, null));
		scope = scope.getParent();
	}

	protected void readEndDocument() {
		queue.add(new Event(XMLStreamConstants.END_DOCUMENT, scope, null));
	}

//	protected XMLStreamReaderScope<T> getScope() {
//		return scope;
//	}

	protected void init() throws XMLStreamException {
		try {
			moreTokens = consume(scope);
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}

		if (hasNext()) {
			event = queue.remove();
		} else {
			event = new Event(XMLStreamConstants.END_DOCUMENT, scope, null);
		}
	}

	/**
	 * Main method to be implemented by subclasses.
	 * @param scope current element scope
	 * @return <code>true</code> if there's more to read
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	protected abstract boolean consume(XMLStreamReaderScope<T> scope) throws XMLStreamException, IOException;

	public void require(int eventType, String namespaceURI, String localName) throws XMLStreamException {
		if (eventType != getEventType()) {
			throw new XMLStreamException("expected event type " + getEventName(eventType) + ", was " + getEventName(getEventType()));
		}
		if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) {
			throw new XMLStreamException("expected namespace " + namespaceURI + ", was " + getNamespaceURI());
		}
		if (localName != null && !localName.equals(getLocalName())) {
			throw new XMLStreamException("expected local name " + localName + ", was " + getLocalName());
		}
	}

	public String getElementText() throws XMLStreamException {
		require(XMLStreamConstants.START_ELEMENT, null, null);
		StringBuilder builder = null;
		String leadText = null;
		while (true) {
			switch (next()) {
			case XMLStreamConstants.CHARACTERS:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.ENTITY_REFERENCE:
				if (leadText == null) { // first event?
					leadText = getText();
				} else {
					if (builder == null) { // second event?
						builder = new StringBuilder(leadText);
					}
					builder.append(getText());
				}
				break;
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
			case XMLStreamConstants.COMMENT:
				break;
			case XMLStreamConstants.END_ELEMENT:
				return builder == null ? leadText : builder.toString();
			default:
				throw new XMLStreamException("Unexpected event type " + getEventName(), getLocation());
			}
		}
	}

	public boolean hasNext() throws XMLStreamException {
		try {
			while (queue.isEmpty() && moreTokens) {
				moreTokens = consume(scope);
			}
		} catch (IOException ex) {
			throw new XMLStreamException(ex);
		}
		return !queue.isEmpty();
	}

	public int next() throws XMLStreamException {
		if (!hasNext()) {
			throw new IllegalStateException("No more events");
		}
		event = queue.remove();
		return event.getType();
	}

	public int nextTag() throws XMLStreamException {
		int eventType = next();
		while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
				|| (eventType == XMLStreamConstants.CDATA && isWhiteSpace()) // skip whitespace
				|| eventType == XMLStreamConstants.SPACE
				|| eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
				|| eventType == XMLStreamConstants.COMMENT) {
			 eventType = next();
		}
		if (!isStartElement() && !isEndElement()) {
			throw new XMLStreamException("expected start or end tag", getLocation());
		}
		return eventType;
	}

	public void close() throws XMLStreamException {
		scope = null;
		queue.clear();
	}

	public boolean isStartElement() {
		return getEventType() == XMLStreamConstants.START_ELEMENT;
	}

	public boolean isEndElement() {
		return getEventType() == XMLStreamConstants.END_ELEMENT;
	}

	public boolean isCharacters() {
		return getEventType() == XMLStreamConstants.CHARACTERS;
	}

	public boolean isWhiteSpace() {
		return false;
	}

	public int getAttributeCount() {
		return event.getScope().getAttributeCount();
	}

	public QName getAttributeName(int index) {
		return event.getScope().getAttributeName(index);
	}

	public String getAttributeLocalName(int index) {
		return getAttributeName(index).getLocalPart();
	}

	public String getAttributeValue(int index) {
		return event.getScope().getAttributeValue(index);
	}

	public String getAttributePrefix(int index) {
		return getAttributeName(index).getPrefix();
	}

	public String getAttributeNamespace(int index) {
		return getAttributeName(index).getNamespaceURI();
	}

	public String getAttributeType(int index) {
		return null;
	}

	public boolean isAttributeSpecified(int index) {
		return index < getAttributeCount();
	}

	public String getAttributeValue(String namespaceURI, String localName) {
		return event.getScope().getAttributeValue(namespaceURI, localName);
	}

	public String getNamespaceURI(String prefix) {
		return event.getScope().getNamespaceURI(prefix);
	}

	public int getNamespaceCount() {
		return hasName() ? event.getScope().getNamespaceCount() : null;
	}

	public String getNamespacePrefix(int index) {
		return hasName() ? event.getScope().getNamespacePrefix(index) : null;
	}

	public String getNamespaceURI(int index) {
		return hasName() ? event.getScope().getNamespaceURI(index) : null;
	}

	public NamespaceContext getNamespaceContext() {
		return event.getScope();
	}

	public int getEventType() {
		return event.getType();
	}

	protected String getEventName() {
		return getEventName(getEventType());
	}
	
	public Location getLocation() {
		return new Location() {
			public int getCharacterOffset() {
				return -1;
			}
			public int getColumnNumber() {
				return -1;
			}
			public int getLineNumber() {
				return -1;
			}
			public String getPublicId() {
				return null;
			}
			public String getSystemId() {
				return null;
			}
		};
	}

	public boolean hasText() {
		return isCharacters();
	}

	public String getText() {
		return hasText() ? event.getText() : null;
	}

	public char[] getTextCharacters() {
		return hasText() ? event.getText().toCharArray() : null;
	}

	public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
		int count = Math.min(length, getTextLength());
		if (count > 0) {
			System.arraycopy(getTextCharacters(), sourceStart, target, targetStart, count);
		}
		return count;
	}

	public int getTextStart() {
		return 0;
	}

	public int getTextLength() {
		return hasText() ? event.getText().length() : 0;
	}

	public boolean hasName() {
		return getEventType() == XMLStreamConstants.START_ELEMENT || getEventType() == XMLStreamConstants.END_ELEMENT;
	}

	public QName getName() {
		return hasName() ? new QName(getNamespaceURI(), getLocalName(), getPrefix()) : null;
	}

	public String getLocalName() {
		return hasName() ? event.getScope().getLocalName() : null;
	}
	
	protected String getTagName() {
		return hasName() ? event.getScope().getTagName() : null;
	}

	public String getNamespaceURI() {
		return hasName() ? event.getScope().getNamespaceURI(event.getScope().getPrefix()) : null;
	}

	public String getPrefix() {
		return hasName() ? event.getScope().getPrefix() : null;
	}

	public String getVersion() {
		return null;
	}

	public String getEncoding() {
		return null;
	}

	public boolean isStandalone() {
		return false;
	}

	public boolean standaloneSet() {
		return false;
	}

	public String getCharacterEncodingScheme() {
		return null;
	}

	public String getPITarget() {
		if (event.getType() != XMLStreamConstants.PROCESSING_INSTRUCTION) {
			return null;
		}
		int colon = event.getText().indexOf(':');
		return colon < 0 ? event.getText() : event.getText().substring(0, colon);
	}

	public String getPIData() {
		if (event.getType() != XMLStreamConstants.PROCESSING_INSTRUCTION) {
			return null;
		}
		int colon = event.getText().indexOf(':');
		return colon < 0 ? null : event.getText().substring(colon + 1);
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		throw new IllegalArgumentException("Unsupported property: " + name);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getEventName() + ")";
	}
}
