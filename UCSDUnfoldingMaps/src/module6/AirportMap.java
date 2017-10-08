package module6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.geo.Location;
import parsing.ParseFeed;
import processing.core.PApplet;

/** An applet that shows airports (and routes)
 * on a world map.  
 * @author Adam Setters and the UC San Diego Intermediate Software Development
 * MOOC team
 *
 */

/**
    The following functions were added into the program:
    - Show points for all airports on the map
    - When mouse hover the airport show a popup box with its code and name
    - When click on airport marker hide all other airports except ones have direct route from this one.
      Show also all the routes started in this airport.

The provided setup() code creates markers for each airport and each route but doesn't store the
relations between them in any way. So the key for the (3) implementation is to maintain some data structure
which would allow to determine which markers should be hidden and which ones displayed once some of the
airport is clicked. This data structure should perform fast and be easy to use.

First of all we need to determine which airport is clicked. To bound markers and airports the id of each airport
was copied to the corresponding marker (id of marker was set to airport id).

Once the id of the clicked airport is determined we need to know which airport and routes markers should remain displayed.
To maintain this information the hash table is used which maps airport ids to list of routes started in airport with this id:
      Map<Integer, List<Route>> routeCache;
Route is a new class which contains two fields:
    - destMarker - marker of the destination airport
    - routeMarker - marker of route

routeCache is filled in the setup() along with creation of the route markers. For each route the Route object is
created and is put to the corresponding list.

To speedup the routeCache creation the intermediate hash table airportCache was also created which map airport ids to markers
during the airport markers creation. This hash map helps to get airport marker by airport id fast.

 */
public class AirportMap extends PApplet {
	
	private UnfoldingMap map;
	private List<Marker> airportList;
	private List<Marker> routeList;
	
    class Route {
        public Marker getAirport() {
            return airport;
        }

        public Marker getRoute() {
            return route;
        }

        private Marker airport;
        private Marker route;

        public Route(Marker airport, Marker route) {
            this.airport = airport;
            this.route = route;
        }
    }
    private Map<Integer, List<Route>> routesCache;


    private CommonMarker lastSelected;
    private CommonMarker lastClicked;

	public void setup() {
		// setting up PAppler
		size(800,600, OPENGL);
		
		// setting up map and default events
		map = new UnfoldingMap(this, 50, 50, 750, 550);
		MapUtils.createDefaultEventDispatcher(this, map);
		
		// get features from airport data
		List<PointFeature> features = ParseFeed.parseAirports(this, "airports.dat");
		
		// list for markers, hashmap for quicker access when matching with routes
		airportList = new ArrayList<Marker>();
        Map<Integer, Marker> airportsCache = new HashMap<>();
		HashMap<Integer, Location> airports = new HashMap<Integer, Location>();
		
		// create markers from features
		for(PointFeature feature : features) {
		    if (feature.properties.containsKey("code")) {

                AirportMarker m = new AirportMarker(feature);

                m.setRadius(5);
                m.setId(feature.getId());
                airportList.add(m);
                airportsCache.put(Integer.parseInt(feature.getId()), m);

                // put airport in hashmap with OpenFlights unique id for key
                airports.put(Integer.parseInt(feature.getId()), feature.getLocation());
            }
		}
		
		
		// parse route data
		List<ShapeFeature> routes = ParseFeed.parseRoutes(this, "routes.dat");
		routeList = new ArrayList<Marker>();
        routesCache = new HashMap<>();
		for(ShapeFeature route : routes) {
			
			// get source and destination airportIds
			int source = Integer.parseInt((String)route.getProperty("source"));
			int dest = Integer.parseInt((String)route.getProperty("destination"));
			
			// get locations for airports on route
			if(airports.containsKey(source) && airports.containsKey(dest)) {
				route.addLocation(airports.get(source));
				route.addLocation(airports.get(dest));
			}
			
			SimpleLinesMarker sl = new SimpleLinesMarker(route.getLocations(), route.getProperties());
		
			//System.out.println(sl.getProperties());
			

            sl.setId(route.getId());
            sl.setHidden(true);

			//UNCOMMENT IF YOU WANT TO SEE ALL ROUTES
			routeList.add(sl);

            if (!routesCache.containsKey(source))
                routesCache.put(source, new ArrayList<>());
            routesCache.get(source).add(new Route(airportsCache.get(dest), sl));
		}
		
		
		
		//UNCOMMENT IF YOU WANT TO SEE ALL ROUTES
		map.addMarkers(routeList);
		
		map.addMarkers(airportList);
		
	}
	
	public void draw() {
		//background(0);
		map.draw();
		
	}
	

    @Override
    public void mouseMoved()
    {
        // clear the last selection
        if (lastSelected != null) {
            lastSelected.setSelected(false);
            lastSelected = null;

        }
        selectMarkerIfHover(airportList);
    }

    @Override
    public void mouseClicked()
    {
        if (lastClicked != null) {
            unhideMarkers(airportList);
            hideMarkers(routeList);
            lastClicked = null;
        }
        else
        {
            lastClicked = (AirportMarker)getClickedMarker(airportList);
            if (lastClicked == null) {
                unhideMarkers(airportList);
                hideMarkers(routeList);
            } else {
                hideAllExcept(airportList, lastClicked);
            }
        }
    }

    private Marker getClickedMarker(List<Marker> markers) {
        for (Marker marker : markers) {
            if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
                return marker;
            }
        }
        return null;
    }

    private void unhideMarkers(List<Marker> markers) {
        for(Marker marker : markers) {
            marker.setHidden(false);
        }
    }

    private void hideMarkers(List<Marker> markers) {
        for(Marker marker : markers) {
            marker.setHidden(true);
        }
    }

    private void hideAllExcept(List<Marker> markers, Marker exception) {
        hideMarkers(airportList);
        exception.setHidden(false);
        if (routesCache.containsKey(Integer.parseInt(exception.getId()))) {
            for (Route route : routesCache.get(Integer.parseInt(exception.getId()))) {
                route.getAirport().setHidden(false);
                route.getRoute().setHidden(false);
            }
        }
//        for (Marker marker : markers) {
//            if (marker != exception) {
//                System.out.println(marker);
//                marker.setHidden(true);
//                routeList.stream()
//                         .filter(r -> r.getStringProperty("source").equals(exception.getId()) &&
//                                               r.getStringProperty("destination").equals(marker.getId()))
//                         .findFirst()
//                         .ifPresent(r -> {
//                             r.setHidden(false);
//                             marker.setHidden(false);
//                         });
//            } else {
//                marker.setHidden(false);
//            }
//        }
    }

    private void selectMarkerIfHover(List<Marker> markers)
    {
        // Abort if there's already a marker selected
        if (lastSelected != null) {
            return;
        }
        for (Marker m : markers)
        {
            CommonMarker marker = (CommonMarker)m;
            if (marker.isInside(map,  mouseX, mouseY)) {
                lastSelected = marker;
                marker.setSelected(true);
                return;
            }
        }
    }
}
