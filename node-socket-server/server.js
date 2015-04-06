var express = require('express'),
    path = require('path'),
    http = require('http'),
    io = require('socket.io');
    logger = require('morgan');
    bodyParser = require('body-parser');
    util = require('util');

var app = express();

app.set('port', process.env.PORT || 8100);
app.use(logger('dev'));
app.use(bodyParser.urlencoded({ extended: true }))
app.use(express.static(path.join(__dirname, 'public')));

// mysql support not yet implemented
var mysql      = require('mysql');
var connection = mysql.createConnection({
  host     : 'localhost',
  user     : '< MySQL username >',
  password : '< MySQL password >',
  database : '<your database name>'
});

var server = http.createServer(app);
io = io.listen(server);
console.log("Server has started.");

var socketClientList = [];
var userData = [];

server.listen(app.get('port'), function () {
    console.log("Express server listening on port " + app.get('port'));
});

/* unused */
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

/* unused */
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

/*io.configure(function () {
    io.set('authorization', function (handshakeData, callback) {
        if (handshakeData.xdomain) {
            callback('Cross-domain connections are not allowed');
        } else {
            callback(null, true);
        }
    });
});*/

// /admin is the admin namespace used for the dashboard
var adminSocket = io.of('/admin');
adminSocket.on('connection', function(socket){
  console.log('Admin connected');
  
  // send all connected users list to admin dashboard
  var usernameData = [];
  console.log('userData length : ' + userData.length);
  for (var key in userData) {
    usernameData.push(userData[key].username);
  }
  socket.emit('receivedata',usernameData);

  socket.on('notifyuser', function(username){
    if(true){ // username validation
      // retreive the socket for the user
      var clientuser = userData.filter(function( el ) {
        console.log(" uName : " + el.username);
        return el.username == username;
      });
      
      console.log('notifyuser ' + username);
      clientuser[0].socketClient.emit('notifyuser','some message');
    }
  });
});

// send client update to admin
var clientupdate = function(data){
  var msg = {
    username : data.username,
    status : data.status,
    connections : data.connections
  };
  io.of('/admin').emit('clientupdate',msg);
};

// send every new location update to admin
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

// all users are connected in the user namespace
var userio = io.of('/users');
userio.on("connection", function(client){

  socketClientList.push(client);

  //console.log("client data" + util.inspect(client));
  //console.log("connect with hostname " + client.hostname);
  client.on('setUserId',function(user_id){

     var user = {
        username : user_id,
        socketClient : client,
        status : 'Active'
     };

    userData.push(user);

    console.log("client connected for user_id: " + user_id);
    client.user_id = user_id;

    clientupdate({username : client.user_id, status : 'Active',connections : userData.length});
  });

  client.on('updatelocation',function(location){
    var data = {
        username : client.user_id,
        latitude : location,
        longitude : '55',
        timestamp : new Date()
    };

    updatelocation(data);
    console.log("location update: " + socketClientList.length + " : " + client.user_id);
  });

  client.on('disconnect', function(){
    console.log("client disconnected for user_id: " + client.user_id);
    socketClientList.splice(socketClientList.indexOf(client), 1);
    userData.splice(userData.indexOf(userData.socketClient));

    // set status of the client as disconnected
    clientupdate({username : client.user_id, status : 'Inactive',connections : socketClientList.length});
  });
});

