# Swarm Enhancer
This Android application will periodically check your current location and automatically check you in to the closest Foursquare venue.

## Parameters
### Update interval
_Update interval_ parameter controls how often the app will check whether your current location changed significantly relative to the _most recent checked-in venue_ in foursquare.

This check is very cheap, since the location of the _most recent checked-in venue_ is cached in the application and no foursquare API requests are required.

Only if the user has moved away from the cached _most recent checked-in venue_ location for more than the _local radius_ parameter, the app searches for a _new venue_ through the foursquare API and also retrieves user's _most recent checked-in venue_. It then checks whether the distance between the _new venue_ and the _most recent checked-in venue_ is larger that the _local radius_ to determine whether a new check-in should be created.

Default value: 4 hours

### Local radius
_local radius_ parameter is used to determine whether a new check-in should be performed. This parameter is used to calculate:
* a distance between user's _current location_ and the _most recent checked-in venue_ in foursquare.
* a distance between the _most recent checked-in venue_ and the _new venue_, which is the first one in foursquare search results.
In both cases the distance must be greater than the _local radius_ parameter for the new check-in to happen. Parameter unit is km.

Default value: 5 km
