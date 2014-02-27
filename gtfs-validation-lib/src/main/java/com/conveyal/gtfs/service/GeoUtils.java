package com.conveyal.gtfs.service;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.opengis.referencing.operation.NoninvertibleTransformException;
import java.util.List;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

 public class GeoUtils {
   public static double RADIANS = 2 * Math.PI;
   
   
   public static MathTransform recentMathTransform = null;
   public static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),4326);
   public static GeometryFactory projectedGeometryFactory = new GeometryFactory(new PrecisionModel());
	 
   public static ProjectedCoordinate convertLatLonToEuclidean(
     Coordinate latlon) {
	   
	   Coordinate lonlat = new Coordinate(latlon.y, latlon.x);
	  
     return convertLonLatToEuclidean(lonlat);
   }
   
   public static ProjectedCoordinate convertLonLatToEuclidean(
     Coordinate lonlat) {
	   
     final MathTransform transform = getTransform(lonlat);
     final Coordinate to = new Coordinate();
    
     // the transform seems to swap the lat lon pairs
     Coordinate latlon = new Coordinate(lonlat.y, lonlat.x);
    
     try {
       JTS.transform(latlon, to,
           transform);
     } catch (final TransformException e) {
       e.printStackTrace();
     }

     return new ProjectedCoordinate(transform, new Coordinate(to.y, to.x), lonlat);
   }

  
   public static Coordinate convertToLatLon(
     MathTransform transform, Coordinate xy) {
	   
	 Coordinate lonlat = convertToLonLat(transform, xy);
     return new Coordinate(lonlat.y, lonlat.x);
   }
   
   public static Coordinate convertToLonLat(
     MathTransform transform, Coordinate xy) {
     final Coordinate to = new Coordinate();
     final Coordinate yx = new Coordinate(xy.y, xy.x);
     try {
     JTS.transform(yx, to, transform.inverse());
     } catch (final TransformException e) {
         e.printStackTrace();
       }
     return new Coordinate(to.y, to.x);
   }

   public static Coordinate convertToLatLon(ProjectedCoordinate pc) {
	   
	   final Coordinate point =
		         new Coordinate(pc.getX(), pc.getY());
	    return convertToLatLon(pc.getTransform(), point);
	  }
   
  

   public static Coordinate convertToLonLat(ProjectedCoordinate pc) {
	   
	   final Coordinate point =
		         new Coordinate(pc.getX(), pc.getY());
	    return convertToLonLat(pc.getTransform(), point);
	  }
  

   /**
    * From
    * http://gis.stackexchange.com/questions/28986/geotoolkit-conversion-from
    * -lat-long-to-utm
    */
   public static int
       getEPSGCodefromUTS(Coordinate refLonLat) {
     // define base EPSG code value of all UTM zones;
     int epsg_code = 32600;
     // add 100 for all zones in southern hemisphere
     if (refLonLat.y < 0) {
       epsg_code += 100;
     }
     // finally, add zone number to code
     epsg_code += getUTMZoneForLongitude(refLonLat.x);

     return epsg_code;
   }


   public static double getMetersInAngleDegrees(
     double distance) {
     return distance / (Math.PI / 180d) / 6378137d;
   }

   public static MathTransform getTransform(
     Coordinate refLatLon) {
     //    MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
     //    ReferencingFactoryContainer factories = new ReferencingFactoryContainer(null);

     try {
       final CRSAuthorityFactory crsAuthorityFactory =
           CRS.getAuthorityFactory(false);
       
       
       final GeographicCRS geoCRS =
           crsAuthorityFactory.createGeographicCRS("EPSG:4326");
//         org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;

       final CoordinateReferenceSystem dataCRS = 
           crsAuthorityFactory
               .createCoordinateReferenceSystem("EPSG:" 
                   + getEPSGCodefromUTS(refLatLon)); //EPSG:32618

       //      parameters = mtFactory.getDefaultParameters("Transverse_Mercator");
       //
       //      final double zoneNumber = zone;
       //      final double utmZoneCenterLongitude = (zoneNumber - 1) * 6 - 180 + 3; // +3 puts origin
       //      parameters.parameter("central_meridian").setValue(utmZoneCenterLongitude);
       //      parameters.parameter("latitude_of_origin").setValue(0.0);
       //      parameters.parameter("scale_factor").setValue(0.9996);
       //      parameters.parameter("false_easting").setValue(500000.0);
       //      parameters.parameter("false_northing").setValue(0.0);
       //  
       //      Map properties = Collections.singletonMap("name", "WGS 84 / UTM Zone " + zoneNumber);
       //      ProjectedCRS projCRS = factories.createProjectedCRS(properties, geoCRS, null, parameters, cartCS);

       final MathTransform transform =
           CRS.findMathTransform(geoCRS, dataCRS);
       
       GeoUtils.recentMathTransform = transform;
       
       return transform;
     } catch (final NoSuchIdentifierException e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     } catch (final FactoryException e) {
       // TODO Auto-generated catch block
       e.printStackTrace();
     }

     return null;
     //    String[] spec = new String[6];
     //    spec[0] = "+proj=utm";
     //    spec[1] = "+zone=" + zone;
     //    spec[2] = "+ellps=clrk66";
     //    spec[3] = "+units=m";
     //    spec[4] = "+datum=NAD83";
     //    spec[5] = "+no_defs";
     //    Projection projection = ProjectionFactory.fromPROJ4Specification(spec);
     //    return projection;
   }

   /*
    * Taken from OneBusAway's UTMLibrary class
    */
   public static int getUTMZoneForLongitude(double lon) {

     if (lon < -180 || lon > 180)
       throw new IllegalArgumentException(
           "Coordinates not within UTM zone limits");

     int lonZone = (int) ((lon + 180) / 6);

     if (lonZone == 60)
       lonZone--;
     return lonZone + 1;
   }

 

  

 }