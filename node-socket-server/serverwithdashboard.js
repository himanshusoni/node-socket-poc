var express = require('express'),
    path = require('path'),
    http = require('http'),
    io = require('socket.io');
    logger = require('morgan');
    bodyParser = require('body-parser'); 
    //wine = require('./routes/wines');

var app = express();

//app.configure(function () {
    app.set('port', process.env.PORT || 8080);
    app.use(logger('dev'));
    app.use(bodyParser.urlencoded({ extended: true }))
    app.use(express.static(path.join(__dirname, 'public')));
//});

var mysql      = require('mysql');
var connection = mysql.createConnection({
  host     : 'localhost',
  user     : '< MySQL username >',
  password : '< MySQL password >',
  database : '<your database name>'
});

var server = http.createServer(app);
io = io.listen(server);
console.log("Server " + server.host + " has started.");

var map_clients = [];
/*io.configure(function () {
    io.set('authorization', function (handshakeData, callback) {
        if (handshakeData.xdomain) {
            callback('Cross-domain connections are not allowed');
        } else {
            callback(null, true);
        }
    });
});*/

server.listen(app.get('port'), function () {
    console.log("Express server listening on port " + app.get('port'));
});

/*app.get('/wines', wine.findAll);
app.get('/wines/:id', wine.findById);
app.post('/wines', wine.addWine);
app.put('/wines/:id', wine.updateWine);
app.delete('/wines/:id', wine.deleteWine);*/

/*io.sockets.on('connection', function (socket) {

    socket.on('message', function (message) {
        console.log("Got message: " + message);
        ip = socket.handshake.address.address;
        url = message;
        io.sockets.emit('pageview', { 'connections': Object.keys(io.connected).length, 'ip': '***.***.***.' + ip.substring(ip.lastIndexOf('.') + 1), 'url': url, 'xdomain': socket.handshake.xdomain, 'timestamp': new Date()});
    });

    socket.on('disconnect', function () {
        console.log("Socket disconnected");
        io.sockets.emit('pageview', { 'connections': Object.keys(io.connected).length});
    });

});*/

function insertuser(user){
  connection.connect(function(err){
    if(err) {
      console.error('error fetching client from pool ', err);
    }
    else{
      var post  = { user_id: user };
      var query = connection.query('INSERT INTO users SET ?', user, function(err, result) {
        // set userid primary key. if exists return it.
        if(err){
          console.error('error inserting ', err);
        }
        else{
          console.log("successfully inserted location");
        }
      });
      console.log(query.sql);
    }
  });
}

/* reuse this to insert into mySQL */
function insertLocation(loc){
  connection.connect(function(err){
    if(err) {
      console.error('error fetching client from pool ', err);
    }
    else{
      //var post  = {id: 1, title: 'Hello MySQL'};
      var query = connection.query('INSERT INTO userlocations SET ?', loc, function(err, result) {
        // Neat!
        if(err){
          console.error('error inserting ', err);
        }
        else{
          console.log("successfully inserted location");
        }
      });
      console.log(query.sql);
    }
  });
}

/* can be ignored for now */
function isAllowed(devices_array, uuid){
  return devices_array.indexOf(uuid) > -1;
}

/*var server = http.createServer(onRequest);
server.listen(port);
console.log("Server " + port + " has started.");

io = io.listen(server);
*/

var adminSocket = io.of('/admin');
adminSocket.on('connection', function(socket){
  console.log('Admin connected');
  var usernameData = [];
  for (var key in map_clients) {
    usernameData.push(key.user_id);
  }
  socket.emit('receivedata',map_clients);

  socket.on('notifyuser', function(user_id){
    if(true){ // check for empty object
      var clientuser = jsObjects.filter(function( obj ) {
        return obj.b == 6;
      });
      clientuser.emit('notifyuser');
    }
  });
});

var clientupdate = function(data){
  var msg = {
    username : data.username,
    status : data.status,
    connections : data.connections
  };
  io.of('/admin').emit('clientupdate',msg);
};

var updatelocation = function(data){
  var msg = {
    username : data.username,
    latitude : data.latitude,
    longitude : data.longitude,
    timestamp : data.timestamp
  };
  console.log('updatelocation to admin');
  io.of('/admin').emit('updatelocation',msg);
};

var userio = io.of('/users');
userio.on("connection", function(client){
  // We push the map clients to an array.
  // If a gps is received from a device,
  // we broadcast the gps to all map clients.
  map_clients.push(client);
  /* push to db */

  console.log("connect with hostname " + client.hostname);
  client.on('setUserId',function(user_id){
    console.log("client connected for user_id: " + user_id);
    client.user_id = user_id;

    // insert the user id in db
    //insertuser(user_id);
    // once user_id is pushed. Add a row to the table and mark status as connected

    // send back join message

    clientupdate({username : client.user_id, status : 'Active',connections : map_clients.length});
  });

  client.on('addDevice',function(device_id){
    console.log("Add device_id: " + device_id);

    if (typeof client.devices == "undefined") {
      client.devices = [];
    }
    client.devices.push(device_id);
  });

  client.on('updatelocation',function(location){
    // uuid ? and user id ?
    //insertLocation(data);
    var data = {
        username : client.user_id,
        latitude : location,
        longitude : '55',
        timestamp : new Date()
    };
    updatelocation(data);
    console.log("location update: " + map_clients.length + " : " + client.user_id);

    // can add a row to the locations updates table
  });

  client.on('disconnect', function(){
    console.log("client disconnected for user_id: " + client.user_id);
    map_clients.splice(map_clients.indexOf(client), 1);

    clientupdate({username : client.user_id, status : 'Inactive',connections : map_clients.length});
    // set status of the client as disconnected
  });
});

