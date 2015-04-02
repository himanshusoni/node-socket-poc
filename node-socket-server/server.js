var http = require('http');
var url = require('url');
var qs = require('querystring');
var io = require('socket.io');
var fs = require('fs');

var port = 8080;

var map_clients = [];

//console.log('connectionString:' + connectionString);
/*
  write mysql connection.
  Use connection pooling later, to allow concurrent users support
  var pool      =    mysql.createPool({
    connectionLimit : 100, //important
    host     : 'localhost',
    user     : 'root',
    password : '',
    database : 'address_book',
    debug    :  false
});
*/
var mysql      = require('mysql');
var connection = mysql.createConnection({
  host     : 'localhost',
  user     : '< MySQL username >',
  password : '< MySQL password >',
  database : '<your database name>'
});

var route = {
  routes : {},
  for: function(method, path, handler){
    this.routes[method + path] = handler;
  }
}

route.for("GET", "/", function(request, response){
  fs.readFile('./index.html', function (err, html) {
    if (err) {
        throw err; 
    }       
        response.writeHeader(200, {"Content-Type": "text/html"});  
        response.write(html);  
        response.end();  
  });
});
route.for("POST", "/location", function(request, response){
  var form_data = "";
  request.on('data', function(chunk){
    form_data += chunk.toString();
  })

console.log("Connected clients in post location : " + map_clients.length);
  request.on('end', function(){
    console.log(form_data);

    var obj = qs.parse(form_data);
    insertLocation(obj);
    console.log("Connected clients: " + map_clients.length);

    for(var i=0; i < map_clients.length; i++){
      var client = map_clients[i];
      console.log("client.user_id:" + client.user_id);
      console.log("client.devices:" + client.devices);

      if (typeof client.devices != "undefined") {
        if(isAllowed(client.devices, obj.uuid)){
          console.log("Sending gps to viewer: " + client.user_id);
          console.log("Devices: " + client.devices);

          var jsonString = JSON.stringify({ type:'gps', data:obj});
          client.send(jsonString);
        }
      }

    }

    response.writeHead(200, {"Content-Type": "text/plain"});
    response.write("OK");
    response.end();
  })
});

function onRequest(request, response){
  var pathname = url.parse(request.url).pathname;
  console.log(request.method + " request for " + pathname);

  if(typeof(route.routes[request.method + pathname]) === 'function'){
    route.routes[request.method + pathname](request, response);
  }
  else{
    response.writeHead(404, {"Content-Type": "text/plain"});
    response.end("404 not found");
  }
}

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

var server = http.createServer(onRequest);
server.listen(port);
console.log("Server " + port + " has started.");

io = io.listen(server);

var adminSocket = io.of('/admin');
adminSocket.on('connection', function(socket){
  console.log('Admin connected');


});

var clientupdate = function(data){
  var msg = {
    username : data.username,
    status : data.connectionStatus,
    connections : data.connections
  };
  adminSocket.emit('clientupdate',msg);
};

var updatelocation = function(data){
  var msg = {
    username : data.username,
    latitude : data.latitude,
    longitude : data.longitude,
    timestamp : data.timestamp
  };
  adminSocket.emit('updatelocation',msg);
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
  });

  client.on('addDevice',function(device_id){
    console.log("Add device_id: " + device_id);

    if (typeof client.devices == "undefined") {
      client.devices = [];
    }

    client.devices.push(device_id);
  });

  client.on('updatelocation',function(data){
    // uuid ? and user id ?
    //insertLocation(data);

    updatelocation(data);
    console.log("location update: " + map_clients.length + " : " + data);

    // can add a row to the locations updates table
  });

  client.on('disconnect', function(){
    console.log("client disconnected for user_id: " + client.user_id);
    map_clients.splice(map_clients.indexOf(client), 1);

    clientupdate({username : client.user_id, status : 'Inactive',connections : map_clients.length});
    // set status of the client as disconnected
  });
});

