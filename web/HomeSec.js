var myvar;

function $(id) {
    return document.getElementById(id);
}

function getHttpRequest(url) {
 
    jQuery.ajax({
        type:"GET",
        url:url,
        dataType:'json',
        beforeSend:function(){
            $('IpLabel').innerHTML="Alle Kameras werden aktualisiert...";
        },
        success:function(result){
            $('IpLabel').innerHTML="";
            buildHTML(result);
            }
    });
}

function postHttpRequest(url) {

    var IpIn=$("ip").value;
    $("ip").value="";
    
    jQuery.ajax({
        type:"POST",
        url:url,
        data:{ip:IpIn},
        dataType:'json',
        beforeSend:function(){
            $('IpLabel').innerHTML="Client "+IpIn+" wird kontaktiert...";
        },
        success:function(result){
            $('IpLabel').innerHTML="";
            buildHTML(result);
        },
        error: function(result){
            if(result.responseText=="error\n") 
                $('IpLabel').innerHTML="Ein Fehler ist aufgetreten!";
            else if (result.responseText=="format\n") 
                $('IpLabel').innerHTML="Ungültiges IP Format!";
            else if (result.responseText=="client\n") 
                $('IpLabel').innerHTML="Client nicht erreichbar!";       
        }
    });
}

function deleteHttpRequest(url) {

    var IdIn=$("ip").value;
    $("ip").value="";
    
    jQuery.ajax({
    type:"DELETE",
    url:url,
    headers:{id:IdIn},
    dataType:'json',
    success:function(result){
            //If servlet returns empty response camera doesnt exist
            if(result == "error\n")
                $('IpLabel').innerHTML="Kamera konnte nicht gelöscht werden!";
        },
    error: function(result){
            if (result.responseText == "id\n")
                $('IpLabel').innerHTML="Bitte Kamera ID angeben!";
            else {
                $('IpLabel').innerHTML="Kamera "+IdIn+" wurde gelöscht!";
                buildHTML(result);
            }
        }
    });
}

//Build HTML for "images" field from passed JSONText
function buildHTML(jsnObj){
   //var jsnObj=JSON.parse(JSONText);
   
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
    //Check if value is negative
    if(refreshVal<0){
        refreshVal=0;
        $('IntervalLabel').innerHTML = "Ungültiger Wert!";
    }
    //deactivate cycle if input is 0 or empty
    if(refreshVal==0 || isNaN(refreshVal)){ 
        $('IntervalLabel').innerHTML = "Intervall ist deaktiviert!";
        clearInterval(myvar);
    }//else calculate the time and set interval
    else{
        var cycle = refreshVal*1000*60;
        myvar=setInterval(function() {
        getHttpRequest('HomeSecServer');
        $('timestamp').innerHTML = new Date().toString()},cycle);
        $('IntervalLabel').innerHTML = "Intervall ist gesetzt!";
    }
}
