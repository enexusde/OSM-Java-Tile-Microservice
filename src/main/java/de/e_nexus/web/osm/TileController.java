/**
 *  _____         _         _           _      _____         _              _____ _ _         
 * |   __|___ ___|_|___ ___| |_ ___ ___| |_   |  |  |___ ___| |_ ___ ___   |_   _|_| |___ ___ 
 * |__   | . |  _| |   | . | . | . | . |  _|  |  |  | -_|  _|  _| . |  _|    | | | | | -_|_ -|
 * |_____|  _|_| |_|_|_|_  |___|___|___|_|     \___/|___|___|_| |___|_|      |_| |_|_|___|___|
 *       |_|           |___| Copyright (c) 2023 Peter Rader
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.                                                                  
 */
package de.e_nexus.web.osm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.e_nexus.web.osm.PostGisPolygon.DoublePoint2D;

@RestController
public class TileController {

	@Inject
	private final DataSource pool = null;

	/**
	 * 
	 * planet_osm_point are places like "Brandenburger Tor". planet_osm_polygon are
	 * areas like "Campingplatz" (where tourism = 'camp_site').
	 * 
	 * @param request
	 * @param response
	 * @param longitude
	 * @param latitude
	 * @param meter
	 * @throws Exception
	 */
	@GetMapping("/tile")
	public String buildings(final HttpServletRequest request, final HttpServletResponse response,
			final double longitude, final double latitude, final float meter, final float dist) throws Exception {
		NamedParameterJdbcTemplate t = new NamedParameterJdbcTemplate(pool);
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("x", longitude);
		namedParameters.addValue("y", latitude);
		namedParameters.addValue("dist", dist);
		namedParameters.addValue("meter", meter);
		PostGisGeometryReader polygonReader = new PostGisGeometryReader();
		String bounding = t.queryForObject("""
				SELECT (
							ST_Transform(
									ST_MakeEnvelope(
										:x - :dist,
										:y + :dist,
										:x + :dist,
										:y - :dist,
										4326
									),
									3857
								)
							) as way
						FROM
							planet_osm_polygon
						LIMIT 1
					""", namedParameters, String.class);
		PostGisPolygon bounds;
		try {
			bounds = polygonReader.readPolygon(new ByteArrayInputStream(bounding.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new IOException("Could not read from '" + bounding.substring(0, 20) + "'", e);
		}
		SqlRowSet areas = t.queryForRowSet("""
				SELECT ST_AsText(result.way) as way, result.name as name FROM
					(
						SELECT
							ST_CollectionExtract(
								ST_ClipByBox2D(
									way,
									ST_Transform(
										ST_MakeEnvelope(
											:x - :dist,
											:y + :dist,
											:x + :dist,
											:y - :dist,
											4326
										),
										3857
									)
								),
								3
						) as way,
						building as name
						FROM
							planet_osm_polygon
						WHERE
							ST_DWithin(
								way,
								ST_Transform(
									ST_SetSRID(
										ST_Point(
											:x,
											:y
										),
										4326
									),
									3857
								),
								:meter
							)
						) as result
					WHERE NOT ST_IsEmpty(result.way) """, namedParameters);
		DoublePoint2D distance = bounds.startBox();
		String html = "<html><body>";

		String divs = "";
		int i = 0;
		while (areas.next()) {
			String way = areas.getString("way");
			String name = areas.getString("name");
			if (name != null)
				System.out.println(name);
			if (polygonReader.polygonPrefixMatch(way)) {
				PostGisPolygon p;
				try {
					p = polygonReader.readPolygon(new ByteArrayInputStream(way.getBytes(StandardCharsets.UTF_8)));
				} catch (IOException e) {
					System.out.println(way);
					throw new IOException("Could not read from '" + way.substring(0, Math.min(20, way.length())) + "'",
							e);
				}
				PostGisPolygon moved = new PostGisPolygon(p, -distance.x, -distance.y);
				divs = createPolyHtml(bounds, divs, name, moved, ++i);
			}
			if (polygonReader.multipolygonPrefixMatch(way)) {
				List<PostGisPolygon> polys = polygonReader
						.readMultipolygon(new ByteArrayInputStream(way.getBytes(StandardCharsets.UTF_8)));
				for (PostGisPolygon postGisPolygon : polys) {
					PostGisPolygon moved = new PostGisPolygon(postGisPolygon, -distance.x, -distance.y);
					divs = createPolyHtml(bounds, divs, name, moved, ++i);
				}
			}
		}

		html = addButton(longitude + 0.001, latitude, meter, dist, html, "Rechts");
		html = addButton(longitude - 0.001, latitude, meter, dist, html, "Links");
		html = addButton(longitude, latitude + 0.001, meter, dist, html, "Hoch");
		html = addButton(longitude, latitude - 0.001, meter, dist, html, "Runter");
		for (int j = 0; j < i; j++) {
			html += "<button onmouseover=\"document.getElementById('id" + j
					+ "').setAttribute('hover','1');\" onmouseout=\"document.getElementById('id" + j
					+ "').removeAttribute('hover');\">" + j + "</button>";
		}
		return html + divs
				+ "<style>div[hover]{background-color: black !important;} .sports_hall, .school, .train_station, .viaduct, .commercial, .hotel, .residential, .house, .garage, .apartments, .garages, .detached{background: rgba(21, 4, 102, 0.23) !important;}</style></body></html>";
	}

	private String addButton(final double longitude, final double latitude, final float meter, final float dist,
			String html, String title) {
		html += "<a href='?longitude=" + longitude + "&latitude=" + latitude + "&meter=" + meter + "&dist=" + dist
				+ "'>" + title + "</a>";
		return html;
	}

	private String createPolyHtml(PostGisPolygon bounds, String html, String name, PostGisPolygon moved, int index) {
		html += ("<div id=\"id" + index + "\" class=\"" + name
				+ "\" style=\"transform: scaleY(-1);position:fixed;background: rgba(1,1,1,0.05); width:"
				+ Math.floor(bounds.getWidth()) + "px; height:" + Math.floor(bounds.getHeight())
				+ "px;clip-path: path('" + moved.toOuterCssClipPath(1f, 0) + "'\"></div>\n");
		return html;
	}
}
