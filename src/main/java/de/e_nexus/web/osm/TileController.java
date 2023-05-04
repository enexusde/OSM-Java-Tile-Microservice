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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TileController {

	public static class PosTile {
		public double longitude, latitude;
		short meter;
	}

	@PersistenceContext
	private final EntityManager entityManager = null;

	@GetMapping("/tile")
	public void tile(final HttpServletRequest request, final HttpServletResponse response, final double longitude,
			final double latitude, final short meter) {
		System.out.println(meter);
		Query q = entityManager.createNativeQuery(
				"SELECT count(*) FROM planet_osm_polygon WHERE ST_Intersects(way, ST_Transform(ST_Buffer(ST_SetSRID(ST_Point(:longitude,:latitude),4326),:meter), 3857));");
		q.setParameter("longitude", longitude);
		q.setParameter("latitude", latitude);
		q.setParameter("meter", meter);
		List resultList = q.getResultList();
		for (Object object : resultList) {
			System.out.println(object);
		}
	}
}
