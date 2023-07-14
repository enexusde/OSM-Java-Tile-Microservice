package de.e_nexus.web.osm;

import java.io.IOException;
import java.io.InputStream;

public class SimpleBracetScopeStreamReader extends InputStream {

	private final InputStream in;

	public SimpleBracetScopeStreamReader(final InputStream in) {
		this.in = in;
	}

	private int inScopes = 0;

	@Override
	public int read() throws IOException {
		int chr = in.read();
		if (chr == '(') {
			inScopes++;
		} else if (chr == ')') {
			inScopes--;
		}
		return chr;
	}

	public int getBracketsReat() {
		return inScopes;
	}

	@Override
	public String toString() {
		return super.toString() + "[bracetsReat=" + inScopes + "]";
	}
}
