package tk.crazysoft.ego.data;

import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.HashMap;

public class Address {
    private static final double METERS_PER_LATITUDE = 111319.5;
    private static final int MIN_STREET_MERGE_METERS = 10000;

    private String zipCode;
    private String city;
    private String street;
    private ArrayList<House> addresses;
    private HashMap<Address, Boolean> distincts;
    private HashMap<Address, Double> distances;

    public Address(String zipCode, String city, String street) {
        this.zipCode = zipCode;
        this.city = city;
        this.street = street;

        this.addresses = new ArrayList<House>();
        this.distincts = new HashMap<Address, Boolean>();
        this.distances = new HashMap<Address, Double>();
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public ArrayList<House> getAddresses() {
        return addresses;
    }

    public boolean isDistinct(Address other) {
        if (distincts.containsKey(other)) {
            return distincts.get(other);
        }
        else {
            boolean result = true;
            outer:		for (House house : addresses) {
                for (House otherHouse : other.addresses) {
                    if (house.getStreetNo().equals(otherHouse.getStreetNo())) {
                        result = false;
                        break outer;
                    }
                }
            }
            distincts.put(other, result);
            other.distincts.put(this, result);
            return result;
        }
    }

    public double getNearestDistance(Address other) {
        if (distances.containsKey(other)) {
            return distances.get(other);
        }
        else {
            double nearestDistance = Double.MAX_VALUE;
            for (House house : addresses) {
                for (House otherHouse : other.addresses) {
                    double mPerLon = getMetersPerLongitude(house.getLocation().y);
                    double othermPerLon = getMetersPerLongitude(otherHouse.getLocation().y);
                    double xDistance = mPerLon * house.getLocation().x - othermPerLon * otherHouse.getLocation().x;
                    double yDistance = METERS_PER_LATITUDE * house.getLocation().y - METERS_PER_LATITUDE * otherHouse.getLocation().y;
                    double distance = Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2));
                    nearestDistance = Math.min(nearestDistance, distance);

                    if (nearestDistance > MIN_STREET_MERGE_METERS) {
                        return Double.POSITIVE_INFINITY;
                    }
                }
            }
            distances.put(other, nearestDistance);
            other.distances.put(this, nearestDistance);
            return nearestDistance;
        }
    }

    private double getMetersPerLongitude(double latitude) {
        // Source: http://southport.jpl.nasa.gov/GRFM/cdrom/2a/DOCS/HTML/GEOLOC/METERS.HTM
        return METERS_PER_LATITUDE * Math.cos(deg2rad(latitude));
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    @Override
    public int hashCode() {
        return zipCode.hashCode() ^ city.hashCode() ^ street.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Address other = (Address) obj;
        if (city == null) {
            if (other.city != null)
                return false;
        } else if (!city.equals(other.city))
            return false;
        if (zipCode == null) {
            if (other.zipCode != null)
                return false;
        } else if (!zipCode.equals(other.zipCode))
            return false;
        if (street == null) {
            if (other.street != null)
                return false;
        } else if (!street.equals(other.street))
            return false;
        return true;
    }

    public static class House {
        private String streetNo;
        private ProjCoordinate location;

        public House(String streetNo, ProjCoordinate location) {
            this.streetNo = streetNo;
            this.location = location;
        }

        public String getStreetNo() {
            return streetNo;
        }

        public ProjCoordinate getLocation() {
            return location;
        }
    }
}
