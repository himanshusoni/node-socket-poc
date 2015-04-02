# node-socket-poc


Contains

1] EGM 	- android application to send location updates to the server.
		- uses socketio java library found at https://github.com/nkzawa/socket.io-client.java
		- contains a background service to update location

2] Nodejs server - Ignore the Read me inside the folder
				- listens to the client for location update
				- add user and location data to db (to be implemented)
				- Admin Panel found at index.html with realtime dashboard.
				- Every client update is broadcasted to the admin namespace.