var myvar;

function $(id) {
    return document.getElementById(id);
}

function getHttpRequest(url) {
    var xmlhttp = null;
    // Mozilla
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    
    xmlhttp.open("GET", url, true);
    $('IpLabel').innerHTML="Alle Kameras werden aktualisiert..."
    xmlhttp.onreadystatechange = function() {
        if(xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            $('IpLabel').innerHTML="";
            buildHTML(xmlhttp.responseText);
        }
    }
    xmlhttp.send(null);
}

function postHttpRequest(url) {
    var xmlhttp = null;
    // Mozilla
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
   
    xmlhttp.open("POST", url, true);
    var IpIn=$("ip").value;
    $("ip").value="";
    $('IpLabel').innerHTML="Client "+IpIn+" wird kontaktiert..."
    xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    
    xmlhttp.onreadystatechange = function() {
         if(xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            $('IpLabel').innerHTML="";
            buildHTML(xmlhttp.responseText);
        }
    }
    xmlhttp.send("ip="+IpIn);
}

function deleteHttpRequest(url) {
    var xmlhttp = null;
    // Mozilla
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.open("DELETE", url, true);
    var id=$("ip").value;
    $("ip").value="";
    
    xmlhttp.setRequestHeader('id', id);
    xmlhttp.onreadystatechange = function() {
         if(xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            //If servlet returns empty response camera doesnt exist
            if(xmlhttp.responseText != "\n"){
                $('IpLabel').innerHTML="Kamera "+id+" wurde gelöscht!";
                buildHTML(xmlhttp.responseText);
            }
            else
                $('IpLabel').innerHTML="Kamera konnte nicht gelöscht werden!";
        }
    }
    xmlhttp.send();
}

//Build HTML for "images" field from passed JSONText
function buildHTML(JSONText){
   var jsnObj=JSON.parse(JSONText);
   
   var html="";   
   for(var i in jsnObj.cameras){
   html+="<h3>Kamera "+jsnObj.cameras[i].id+": "+jsnObj.cameras[i].ip+"</h3>";
   html+="<img id=\"cam"+jsnObj.cameras[i].id+"\" src=\""+
           jsnObj.cameras[i].path+"\" alt=\"Bild Kamera "+
           jsnObj.cameras[i].id+"\" width=\"384\" height=\"216\" >\n<br>";
   }         
   
   $('images').innerHTML=html;
   
}

//Set cyle to refresh camera pictures
function setRefreshCycle(){
    var refreshVal=parseInt($('refresh').value);   
    //deactivate cycle if input is 0 or empty
    if(refreshVal==0 || isNaN(refreshVal)){ 
        $('IntervalLabel').innerhTML = "Intervall ist deaktiviert!";
        clearInterval(myvar);
    }//else calculate the time and set interval
    else{
        var cycle = refreshVal*1000*60;
        myvar=setInterval(function() {
        //getHttpRequest('HomeSecServer');
        $('timestamp').innerHTML = new Date().toString()},cycle);
        $('IntervalLabel').innerhTML = "Intervall ist gesetzt!";
    }
}