/*
 * Sleuth Kit Data Model
 *
 * Copyright 2020 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.datamodel.blackboardutils.attributes;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.blackboardutils.attributes.TskGeoTrackpointsUtil.GeoTrackPointList;
import org.sleuthkit.datamodel.blackboardutils.attributes.TskGeoTrackpointsUtil.GeoTrackPointList.GeoTrackPoint;

/**
 * Utility class for translating TSK_GEO_TRACKPOINTS attribute values to
 * GeoTrackPointList objects and GeoTrackPointList to BlackboardAttributes.
 */
public final class TskGeoTrackpointsUtil implements BlackboardAttributeUtil<GeoTrackPointList> {

	@Override
	public BlackboardAttribute toAttribute(String moduleName, GeoTrackPointList value) {

		if (value == null) {
			throw new IllegalArgumentException("toAttribute was passed a null list");
		}

		return new BlackboardAttribute(
				BlackboardAttribute.ATTRIBUTE_TYPE.TSK_GEO_TRACKPOINTS,
				moduleName,
				toJSON(value));
	}

	@Override
	public GeoTrackPointList fromAttribute(BlackboardAttribute attribute) {
		if (attribute == null) {
			throw new IllegalArgumentException("fromAttribute was passed a null attribute");
		}

		BlackboardAttribute.ATTRIBUTE_TYPE type = BlackboardAttribute.ATTRIBUTE_TYPE.fromID(attribute.getAttributeType().getTypeID());
		if (type != BlackboardAttribute.ATTRIBUTE_TYPE.TSK_GEO_TRACKPOINTS) {
			throw new IllegalArgumentException(String.format("Invalid attribute of type %s passed to fromAttribute method. Attribute of type TSK_GEO_TRACKPOINTS is required", type.getDisplayName()));
		}

		return fromJSON(attribute.getValueString());
	}

	/**
	 * Creates a GeoTrackPointList from the given JSON string.
	 *
	 * @param jsonString JSon string of track points.
	 *
	 * @return	Timestamp ordered list of GeoTrackPoints, empty list will be
	 *			returned if jsonString is null or empty.
	 */
	private static GeoTrackPointList fromJSON(String jsonString) {
		if (jsonString == null || jsonString.isEmpty()) {
			return null;
		}

		return (new Gson()).fromJson(jsonString, GeoTrackPointList.class);
	}

	/**
	 * Returns a JSON string representing the given object.
	 *
	 * @return JSON string
	 */
	private static String toJSON(GeoTrackPointList pointList) {
		Gson gson = new Gson();
		return gson.toJson(pointList);
	}

	/**
	 * A list of GeoTrackPoints.
	 */
	public static class GeoTrackPointList implements Iterable<GeoTrackPointList.GeoTrackPoint> {

		private final List<GeoTrackPoint> pointList;

		/**
		 * Construct an empty GeoTrackPointList.
		 */
		public GeoTrackPointList() {
			pointList = new ArrayList<>();
		}

		/**
		 * Construct a new instance with the given list of GeoTrackPoint
		 * objects.
		 *
		 * @param points List of track points, cannot be null.
		 */
		public GeoTrackPointList(List<GeoTrackPoint> points) {
			if (points == null) {
				throw new IllegalArgumentException("Constructor was passed a null list");
			}

			pointList = points;
		}

		/**
		 * Add a point to the list of track points.
		 *
		 * @param point A point to add to the track point list, cannot be null.
		 */
		public void addPoint(GeoTrackPoint point) {
			if (point == null) {
				throw new IllegalArgumentException("addPoint was passed a null list");
			}

			pointList.add(point);
		}

		/**
		 * Adds a new point with the given attributes.
		 *
		 * @param latitude			    Latitude of the trackpoint, required
		 * @param longitude			    Longitude of the trackpoint, required
		 * @param altitude			    Altitude of the trackpoint, maybe null
		 * @param name				    Name of trackpoint, maybe null
		 * @param velocity				Velocity in meters/sec, maybe null
		 * @param distanceFromHomePoint	Track point distance from an established
		 *                              "home point", may be null if not
		 *                              applicable
		 * @param distanceTraveled			   Overall distance traveled in meters at
		 *                              the time this trackpoint was created,
		 *                              maybe null if not applicable
		 * @param timestamp					        Trackpoint creation time, maybe null if
		 *                              not applicable
		 */
		public void addPoint(Double latitude,
				Double longitude,
				Double altitude,
				String name,
				Double velocity,
				Double distanceFromHomePoint,
				Double distanceTraveled,
				Long timestamp) {
			pointList.add(new GeoTrackPoint(
					latitude,
					longitude,
					altitude,
					name,
					velocity,
					distanceFromHomePoint,
					distanceTraveled,
					timestamp));
		}

		/**
		 * Returns an iterator over the points in this GeoTrackPointList.
		 *
		 * @return An iterator over the elements of the list.
		 */
		@Override
		public Iterator<GeoTrackPoint> iterator() {
			return pointList.iterator();
		}

		/**
		 * Returns true if this list contains no points.
		 *
		 * @return True if this list contains no points.
		 */
		public boolean isEmpty() {
			return pointList.isEmpty();
		}

		/**
		 * Return the start time for the track.
		 *
		 * @return First non-null time stamp or null, if one was not found.
		 */
		public Long getStartTime() {
			List<GeoTrackPoint> orderedPoints = getTimeOrderedPoints();
			if (orderedPoints != null) {
				for (GeoTrackPoint point : orderedPoints) {
					if (point.getTimeStamp() != null) {
						return point.getTimeStamp();
					}
				}
			}
			return null;
		}

		/**
		 * Return the ends time for the track.
		 *
		 * @return First non-null time stamp or null, if one was not found.
		 */
		public Long getEndTime() {
			List<GeoTrackPoint> orderedPoints = getTimeOrderedPoints();
			if (orderedPoints != null) {
				for (int index = orderedPoints.size() - 1; index >= 0; index--) {
					GeoTrackPoint point = orderedPoints.get(index);
					if (point.getTimeStamp() != null) {
						return point.getTimeStamp();
					}
				}
			}
			return null;
		}

		/**
		 * Returns a timestamp ordered copy of the points list.
		 *
		 * @return List of points sorted by timestamps.
		 */
		private List<GeoTrackPoint> getTimeOrderedPoints() {
			return pointList.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
		}

		/**
		 * A GeoTrackPoint is a Waypoint with more detailed information about
		 * the point.
		 *
		 */
		public final static class GeoTrackPoint extends TskGeoWaypointsUtil.GeoWaypointList.GeoWaypoint implements Comparable<GeoTrackPointList.GeoTrackPoint> {

			private final Double velocity;
			private final Double distanceFromHomePoint;
			private final Double distanceTraveled;
			private final Long timestamp;

			/**
			 * Constructs a GeoTrackPoint with the given attributes.
			 *
			 * @param latitude				Latitude of the track point, required
			 * @param longitude				Longitude of the track point,
			 *                              required
			 * @param altitude			    Altitude of the track point, may be
			 *                              null
			 * @param name				    Name of track point, may be null
			 * @param velocity			    Velocity in meters/sec, may be null
			 * @param distanceFromHomePoint	Track point distance from an
			 *                              established "home point", maybe null
			 *                              if not applicable
			 * @param distanceTraveled	    Overall distance traveled in meters
			 *                              at the time this track point was
			 *                              created, maybe null if not
			 *                              applicable
			 * @param timestamp			    Track point creation time, maybe
			 *                              null if not applicable
			 */
			public GeoTrackPoint(Double latitude,
					Double longitude,
					Double altitude,
					String name,
					Double velocity,
					Double distanceFromHomePoint,
					Double distanceTraveled,
					Long timestamp) {
				super(latitude, longitude, altitude, name);
				this.velocity = velocity;
				this.distanceFromHomePoint = distanceFromHomePoint;
				this.distanceTraveled = distanceTraveled;
				this.timestamp = timestamp;
			}

			/**
			 * Returns velocity of the point.
			 *
			 * @return Double velocity value, maybe null if not available or
			 *         applicable
			 */
			public Double getVelocity() {
				return velocity;
			}

			/**
			 * Returns distance from home point for the point.
			 *
			 * @return Double velocity distance from home point, maybe null if
			 *         not available or applicable
			 */
			public Double getDistanceFromHomePoint() {
				return distanceFromHomePoint;
			}

			/**
			 * Returns distance traveled for the point.
			 *
			 * @return Double distance traveled value, maybe null if not
			 *         available or applicable
			 */
			public Double getDistanceTraveled() {
				return distanceTraveled;
			}

			/**
			 * Returns the time stamp (seconds from java/unix epoch) of the
			 * track point.
			 *
			 * @return time stamp of the track point, or null if not available
			 */
			public Long getTimeStamp() {
				return timestamp;
			}

			@Override
			public int compareTo(GeoTrackPointList.GeoTrackPoint otherTP) {
				Long otherTimeStamp = otherTP.getTimeStamp();

				if (timestamp == null && otherTimeStamp != null) {
					return -1;
				} else if (timestamp != null && otherTimeStamp == null) {
					return 1;
				} else {
					return timestamp.compareTo(otherTP.getTimeStamp());
				}
			}
		}
	}
}