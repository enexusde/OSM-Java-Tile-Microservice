package de.e_nexus.web.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.e_nexus.web.osm.PostGisPolygon.DoublePoint2D;

public class PostGisPolygon implements Iterable<DoublePoint2D[]> {

	public static class DoublePoint2D {
		public final double x;
		public final double y;

		public DoublePoint2D(final double x, final double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return x + "x" + y;
		}

		public DoublePoint2D translate(final double translateX, final double translateY) {
			return new DoublePoint2D(x + translateX, y + translateY);
		}
	}

	/**
	 * The geometry data.
	 * 
	 * @param firstPolygonWithoutBrackets The outer geometry, in example "0 0, 10 0,
	 *                                    10 10, 0 10".
	 * @param holesWithoutBrackets        The list of cut-outs, in example {"2 2, 2
	 *                                    8, 8 8, 8 2"}
	 */
	private final DoublePoint2D[] outer;
	private final DoublePoint2D[][] holes;
	private DoublePoint2D start, end;
	private double height = -1d;
	private double width = -1d;

	/**
	 * Create the polygon.
	 * 
	 * @param firstPolygonWithoutBrackets The outer polygon as a list of
	 *                                    coordinates, never <code>null</code>
	 * @param holesWithoutBrackets        The inner cut-outs as a list of
	 *                                    coordinates, never <code>null</code>, may
	 *                                    be empty
	 */
	public PostGisPolygon(final String firstPolygonWithoutBrackets, final String[] holesWithoutBrackets) {
		List<DoublePoint2D> l = getPointlist(firstPolygonWithoutBrackets);
		List<DoublePoint2D[]> cutOutHoles = new ArrayList<>();
		for (String holeString : holesWithoutBrackets) {
			List<DoublePoint2D> hole = getPointlist(holeString);
			cutOutHoles.add(hole.toArray(new DoublePoint2D[0]));
		}
		holes = cutOutHoles.toArray(new DoublePoint2D[0][0]);
		outer = l.toArray(new DoublePoint2D[0]);
	}

	public PostGisPolygon(final PostGisPolygon blueprint, final double translateX, final double translateY) {
		this.outer = new DoublePoint2D[blueprint.outer.length];
		this.holes = new DoublePoint2D[blueprint.holes.length][];
		for (int i = 0; i < outer.length; i++) {
			DoublePoint2D point = blueprint.outer[i];
			outer[i] = new DoublePoint2D(point.x, point.y).translate(translateX, translateY);
		}
		for (int i = 0; i < holes.length; i++) {
			DoublePoint2D[] blueprintPoints = blueprint.holes[i];
			holes[i] = new DoublePoint2D[blueprintPoints.length];
			for (int j = 0; j < blueprintPoints.length; j++) {
				DoublePoint2D blueprintPoint = blueprintPoints[j];
				holes[i][j] = blueprintPoint.translate(translateX, translateY);
			}
		}
	}

	private List<DoublePoint2D> getPointlist(final String firstPolygonWithoutBrackets) {
		List<DoublePoint2D> l = new ArrayList<>();
		String soFaar = "";
		Double x = null;
		for (int i = 0; i < firstPolygonWithoutBrackets.length(); i++) {
			char c = firstPolygonWithoutBrackets.charAt(i);
			if (c >= '0' && c <= '9' || c == '.') {
				soFaar += c;
			} else if ((c == ' ' || c == ',') && !soFaar.isEmpty()) {
				if (x == null) {
					x = Double.parseDouble(soFaar);
				} else {
					l.add(new DoublePoint2D(x, Double.parseDouble(soFaar)));
					x = null;
				}
				soFaar = "";
			}
		}
		l.add(new DoublePoint2D(x, Double.parseDouble(soFaar)));
		return l;
	}

	public int getPointCount() {
		return outer.length;
	}

	public DoublePoint2D getPoint(final int i) {
		return outer[i];
	}

	public int getCutOuts() {
		return holes.length;
	}

	public int getCutoutPointCount(final int cutout) {
		return holes[cutout].length;
	}

	public DoublePoint2D getCutoutPoint(final int cutout, final int point) {
		return holes[cutout][point];
	}

	@Override
	public String toString() {
		String t = "";
		t += Arrays.toString(outer);
		if (holes != null && holes.length > 0) {
			t += "holes";
			for (DoublePoint2D[] points : holes) {
				t += Arrays.toString(points);
			}
		}
		return super.toString() + t;
	}

	@Override
	public Iterator<DoublePoint2D[]> iterator() {
		return new Iterator<PostGisPolygon.DoublePoint2D[]>() {
			public int pos = 0;

			@Override
			public boolean hasNext() {
				return pos < holes.length;
			}

			@Override
			public DoublePoint2D[] next() {
				if (hasNext()) {
					return null;
				}
				return holes[pos++];
			}
		};
	}

	public DoublePoint2D startBox() {
		if (start == null) {
			synchronized (this) {
				if (start == null) {
					double maxx = Double.MAX_VALUE, maxy = Double.MAX_VALUE;
					for (DoublePoint2D point : outer) {
						if (point.x < maxx) {
							maxx = point.x;
						}
						if (point.y < maxy) {
							maxy = point.y;
						}
					}
					start = new DoublePoint2D(maxx, maxy);
					start.toString();
				}
			}
		}
		return start;
	}

	public DoublePoint2D endBox() {
		if (end == null) {
			synchronized (this) {
				if (end == null) {
					double minx = Double.MIN_VALUE, miny = Double.MIN_VALUE;
					for (DoublePoint2D point : outer) {
						if (point.x > minx) {
							minx = point.x;
						}
						if (point.y > miny) {
							miny = point.y;
						}
					}
					end = new DoublePoint2D(minx, miny);
					end.toString();
				}
			}
		}
		return end;
	}

	public String toOuterCssClipPath(final float scale, final int decimalPlaces) {
		String path = "";
		for (DoublePoint2D point : outer) {
			if (path.isEmpty()) {
				path += "M";
			} else {
				path += "L";
			}
			path += calc(point.x, scale, decimalPlaces);
			path += " ";
			path += calc(point.y, scale, decimalPlaces);
		}
		return path;
	}

	private Number calc(final double d, final float scale, final int decimalPlaces) {
		double x = d * scale;
		double num = decimalPlaces == 0 ? 1 : 10 ^ decimalPlaces;
		Number v = Math.round(x * num) / num;
		return v.doubleValue() == v.intValue() ? v.intValue() : v;
	}

	public double getWidth() {
		return endBox().x - startBox().x;
	}

	public double getHeight() {
		return endBox().y - startBox().y;
	}
}
