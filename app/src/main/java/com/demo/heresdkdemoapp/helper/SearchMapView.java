package com.demo.heresdkdemoapp.helper;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.demo.heresdkdemoapp.R;
import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.CustomMetadataValue;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.Camera;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PickMapItemsCallback;
import com.here.sdk.mapviewlite.PickMapItemsResult;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.Maneuver;
import com.here.sdk.routing.ManeuverAction;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.CategoryQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.PlaceCategory;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SuggestCallback;
import com.here.sdk.search.Suggestion;
import com.here.sdk.search.TextQuery;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchMapView {

    private static final String LOG_TAG = SearchMapView.class.getName();

    private final Context context;
    private final MapViewLite mapView;
    private final Camera camera;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private final List<MapPolyline> mapPolylines = new ArrayList<>();
    private SearchEngine searchEngine;
    private RoutingEngine routingEngine;

    //Picked Restaurant
    String title , vicinity;
    double destinationLatitude;
    double destinationLongitude;
    double  currentLocationLat, currentLocationLong;


    GeoCoordinates startGeoCoordinate , destinationGeoCoordinates;
    public SearchMapView(Context context, MapViewLite map, double currentLat, double currentLong){
        this.context = context;
        this.mapView = map;
        camera = mapView.getCamera();

        currentLocationLat = 19.2227;
        currentLocationLong = 72.9833;
        camera.setTarget(new GeoCoordinates(19.2227, 72.9833));
        camera.setZoomLevel(14);

        try{
            searchEngine = new SearchEngine();
        }catch (Exception e){
            e.printStackTrace();
        }

        setTapGestureHandler();
    }

    public void setTapGestureHandler(){
        mapView.getGestures().setTapListener(touchPoint -> pickMapMarker(touchPoint));
    }

    private void pickMapMarker(final Point2D point2D) {
        float radiusInPixel = 2;
        mapView.pickMapItems(point2D, radiusInPixel, new PickMapItemsCallback() {
            @Override
            public void onMapItemsPicked(@Nullable PickMapItemsResult pickMapItemsResult) {
                if (pickMapItemsResult == null) {
                    return;
                }

                MapMarker topmostMapMarker = pickMapItemsResult.getTopmostMarker();
                if (topmostMapMarker == null) {
                    return;
                }

                Metadata metadata = topmostMapMarker.getMetadata();
                if (metadata != null) {
                    CustomMetadataValue customMetadataValue = metadata.getCustomValue("key_search_result");
                    if (customMetadataValue != null) {
                        SearchResultMetadata searchResultMetadata = (SearchResultMetadata) customMetadataValue;
                        title = searchResultMetadata.searchResult.getTitle();
                        vicinity = searchResultMetadata.searchResult.getAddress().addressText;


                    }
                }


                destinationLatitude =  topmostMapMarker.getCoordinates().latitude;
                destinationLongitude = topmostMapMarker.getCoordinates().longitude;

                Log.d("Destination",destinationLatitude + " " + destinationLongitude);
            }
        });
        searchRoute();
    }

    public void searchPlaceCategory(String value){
        List<PlaceCategory> catergoryList = new ArrayList<>();
        if(value.equalsIgnoreCase("Restaurant") || value.equalsIgnoreCase("Restaurants")){

            catergoryList.add(new PlaceCategory(PlaceCategory.EAT_AND_DRINK_RESTAURANT));
            catergoryList.add(new PlaceCategory(PlaceCategory.EAT_AND_DRINK));


        }
        else{
            catergoryList.add(new PlaceCategory(PlaceCategory.SHOPPING_FOOD_AND_DRINK));
            catergoryList.add(new PlaceCategory(PlaceCategory.GOING_OUT_ENTERTAINMENT));
        }
        CategoryQuery categoryQuery = new CategoryQuery(catergoryList, new GeoCoordinates(19.2183, 72.9781));
        int maxItems = 30;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        searchEngine.search(categoryQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                if(searchError != null){
                    Toast.makeText(context,"Error while Searching  "+searchError.toString(),Toast.LENGTH_SHORT).show();
                }else{
                    Log.d("Search Result " , " "+ list.size());
                    for(Place searchResult : list) {
                        Metadata metadata = new Metadata();
                        metadata.setCustomValue("key_search_result", new SearchResultMetadata(searchResult));
                        addMarker(searchResult.getGeoCoordinates(), metadata);
                    }
                }
            }
        });
    }



    private void addMarker(GeoCoordinates geoCoordinates, Metadata metadata) {
        MapMarker mapMarker = createPoiMapMarker(geoCoordinates);
        mapMarker.setMetadata(metadata);
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    private MapMarker createPoiMapMarker(GeoCoordinates geoCoordinates) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), R.drawable.poi);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));
        mapMarker.addImage(mapImage, mapMarkerImageStyle);
        return mapMarker;
    }

    private static class SearchResultMetadata implements CustomMetadataValue {

        public final Place searchResult;

        public SearchResultMetadata(Place searchResult) {
            this.searchResult = searchResult;
        }

        @NonNull
        @Override
        public String getTag() {
            return "SearchResult Metadata";
        }
    }
    public void searchPlace(String value){
        clearMarkerOnMap();

        GeoBox geoBox = mapView.getCamera().getBoundingBox();
        TextQuery textQuery = new TextQuery(value, geoBox);
        int maxItems = 30;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        searchEngine.search(textQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                if(searchError != null){
                    Toast.makeText(context,"Error while Searching  "+searchError.toString(),Toast.LENGTH_SHORT).show();
                }else{
                    Log.d("Search Result " , " "+ list.size());
                }
            }
        });

        autoSuggestExample();
    }

    public void searchRoute(){
       try {
           routingEngine = new RoutingEngine();
       }catch (Exception e){
           e.printStackTrace();
       }

        startGeoCoordinate = new GeoCoordinates(currentLocationLat, currentLocationLong);
       destinationGeoCoordinates = new GeoCoordinates(destinationLatitude, destinationLongitude);

        Waypoint startWaypoint = new Waypoint(startGeoCoordinate);
        Waypoint destinationWaypoint = new Waypoint(destinationGeoCoordinates);

        List<Waypoint> waypoints =
                new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(
                waypoints,
                new CarOptions(),
                new CalculateRouteCallback() {

            @Override
            public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                if(routingError == null){
                    Route route = routes.get(0);
                    showRouteDetails(route);
                    showRouteOnMap(route);

                }else {
                    Toast.makeText(context,"Error while plotting Routine ",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void showRouteOnMap(Route route) {
        // Show route as polyline.
        GeoPolyline routeGeoPolyline;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            // It should never happen that the route polyline contains less than two vertices.
            return;
        }
        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapPolylineStyle.setWidthInPixels(10);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

        // Draw a circle to indicate starting point and destination.
        addCircleMapMarker(startGeoCoordinate, R.drawable.green_dot);
        addCircleMapMarker(destinationGeoCoordinates, R.drawable.green_dot);

        // Log maneuver instructions per route section.
        List<Section> sections = route.getSections();
        for (Section section : sections) {
            logManeuverInstructions(section);
        }
    }

    private void logManeuverInstructions(Section section) {
        Log.d("Routing", "Log maneuver instructions per route section:");
        List<Maneuver> maneuverInstructions = section.getManeuvers();
        for (Maneuver maneuverInstruction : maneuverInstructions) {
            ManeuverAction maneuverAction = maneuverInstruction.getAction();
            GeoCoordinates maneuverLocation = maneuverInstruction.getCoordinates();
            String maneuverInfo = maneuverInstruction.getText()
                    + ", Action: " + maneuverAction.name()
                    + ", Location: " + maneuverLocation.toString();
            Log.d("Routing", maneuverInfo);
        }
    }

    private void addCircleMapMarker(GeoCoordinates geoCoordinates, int resourceId) {
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), resourceId);
        MapMarker mapMarker = new MapMarker(geoCoordinates);
        mapMarker.addImage(mapImage, new MapMarkerImageStyle());
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }


    private void showRouteDetails(Route route) {
        long estimatedTravelTimeInSeconds = route.getDurationInSeconds();
        int lengthInMeters = route.getLengthInMeters();

        String routeDetails =
                "Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                        + ", Length: " + formatLength(lengthInMeters);

        Toast.makeText(context,"Route Details "+routeDetails,Toast.LENGTH_SHORT);
    }

    private String formatTime(long sec) {
        long hours = sec / 3600;
        long minutes = (sec % 3600) / 60;

        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    private String formatLength(int meters) {
        int kilometers = meters / 1000;
        int remainingMeters = meters % 1000;

        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters);
    }

    private final SuggestCallback autosuggestCallback = new SuggestCallback() {
        @Override
        public void onSuggestCompleted(@Nullable SearchError searchError, @Nullable List<Suggestion> list) {
            if (searchError != null) {
                Log.d(LOG_TAG, "Autosuggest Error: " + searchError.name());
                return;
            }

            // If error is null, list is guaranteed to be not empty.
            Log.d(LOG_TAG, "Autosuggest results: " + list.size());

            for (Suggestion autosuggestResult : list) {
                String addressText = "Not a place.";
                Place place = autosuggestResult.getPlace();
                if (place != null) {
                    addressText = place.getAddress().addressText;
                }

                Log.d(LOG_TAG, "Autosuggest result: " + autosuggestResult.getTitle() +
                        " addressText: " + addressText);
            }
        }
    };

    private void autoSuggestExample() {
        GeoCoordinates centerGeoCoordinates = mapView.getCamera().getTarget();
        int maxItems = 5;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);

        // Simulate a user typing a search term.
        searchEngine.suggest(
                new TextQuery("p", // User typed "p".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);

        searchEngine.suggest(
                new TextQuery("pi", // User typed "pi".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);

        searchEngine.suggest(
                new TextQuery("piz", // User typed "piz".
                        centerGeoCoordinates),
                searchOptions,
                autosuggestCallback);
    }


    private void clearMarkerOnMap(){
        for(MapMarker mapMarker: mapMarkerList){
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkerList.clear();
    }
}
