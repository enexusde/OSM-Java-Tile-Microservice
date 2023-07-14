package de.e_nexus.web.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PostGisGeometryReader {
	private static final String POLYGON_HEAD = "POLYGON((";
	private static final String MULTIPOLYGON_HEAD = "MULTIPOLYGONPOLYGON(((";

	public PostGisPolygon readPolygon(final InputStream genuineReader) throws IOException {
		SimpleBracetScopeStreamReader reader = readHead(genuineReader, POLYGON_HEAD.toCharArray());
		String main = "";
		while (reader.getBracketsReat() == 2) {
			int chr = reader.read();
			if ((chr >= '0' && chr <= '9') | chr == ' ' | chr == '.' | chr == ',') {
				main += (char) chr;
			}
		}
		List<String> lst = new ArrayList<>();
		String hole = "";
		while (reader.getBracketsReat() != 0) {
			int chr = reader.read();
			if (reader.getBracketsReat() == 1 && !hole.isEmpty()) {

				lst.add(hole);
				hole = "";
			} else if (reader.getBracketsReat() == 2
					& ((chr >= '0' && chr <= '9') | chr == ' ' | chr == '.' | chr == ',')) {
				hole += (char) chr;
			}
		}

		return new PostGisPolygon(main, lst.toArray(new String[0]));
	}

	private SimpleBracetScopeStreamReader readHead(final InputStream genuineReader, char[] headChars)
			throws IOException {
		SimpleBracetScopeStreamReader reader = new SimpleBracetScopeStreamReader(genuineReader);
		for (char c : headChars) {
			int reat = reader.read();
			if (reat != c) {
				throw new IOException("Expected to read the character '" + c + "' (in " + POLYGON_HEAD
						+ ") but got char '" + (char) reat + "' (" + reat + ")");
			}
		}
		return reader;
	}

	public boolean polygonPrefixMatch(String way) {
		return way.startsWith(POLYGON_HEAD);
	}

	public boolean multipolygonPrefixMatch(String way) {
		return way.startsWith(MULTIPOLYGON_HEAD);
	}

	public List<PostGisPolygon> readMultipolygon(final InputStream genuineReader) throws IOException {
		SimpleBracetScopeStreamReader reader = readHead(genuineReader, MULTIPOLYGON_HEAD.toCharArray());
		String currentValue = "";
		List<PostGisPolygon> polys = new ArrayList<>();
		while (reader.available() > 0) {
			int read = reader.read();
			if (reader.getBracketsReat() == 3) {
				currentValue += (char) read;
			} else if (reader.getBracketsReat() == 1) {
				polys.add(new PostGisPolygon(currentValue, new String[0]));
			}
		}
		return polys;
	}

}
