<%-- 
    Document   : index
    Created on : 26/10/2015, 11:21:19 AM
    Author     : JSolorzanoS
--%>

<%@page import="org.json.simple.JSONObject"%>
<%@page import="conexion.OdooBridge"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Concordancias</title>
        <script src="js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/smoothness/jquery-ui.css" type="text/css" media="all" />
        <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js" type="text/javascript"></script>
        <script src="js/index.js" type="text/javascript"></script>
        <link rel="stylesheet" href="css/index.css"/>
        <link href='http://fonts.googleapis.com/css?family=Montserrat' rel='stylesheet' type='text/css'>
        <link href='http://fonts.googleapis.com/css?family=Lato' rel='stylesheet' type='text/css'>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" integrity="sha512-dTfge/zgoMYpP7QbHy4gWMEGsbsdZeCXz7irItjcC3sPUFtf0kuFbDz/ixG7ArTxmDjLXDmezHubeNikyKGVyQ==" crossorigin="anonymous">
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css" integrity="sha384-aUGj/X2zp5rLCbBxumKTCw2Z50WgIr1vs/PFN4praOTvYXWlVyh2UtNUU0KAUhAX" crossorigin="anonymous">
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js" integrity="sha512-K1qjQ+NcF2TYO/eI3M6v8EiNYZfA95pQumfvcVrTHtwQVDG+aHRqLi/ETn2uB+1JqwYqVG3LIvdm9lj6imS/pQ==" crossorigin="anonymous"></script>
        <script src="vendor/jqplot/jquery.jqplot.min.js"></script>
        <script src="vendor/jqplot/plugins/jqplot.pieRenderer.js"></script>
        <link rel="stylesheet" href="vendor/jqplot/jquery.jqplot.min.css"/>
    </head>
    <% String projectId = request.getParameter("pid");
        JSONObject user = OdooBridge.getUser(request);
    %>
    <body>

        <% if (request.getParameter("embed") == null) { %>
        <div class="containerr">
            <div class="embed-hide small pull-right user-band">
                <span>
                    <% if ("public".equals(user.get("login"))) {%>
                    Usuario anónimo.
                    <a href="<%= OdooBridge.getURLWithRedirect(request, OdooBridge.REGISTER_URL)%>">Regístrate</a>
                    o
                    <a href="<%= OdooBridge.getURLWithRedirect(request, OdooBridge.LOGIN_URL)%>">Inicia sesión</a>
                    <% } else {%>
                    Conectado como <%= user.get("name")%> |
                    <a href="logout">Cerrar sesión</a>
                    <% } %>
                </span>
            </div>

            <div id="header">

                <h1 class="title">Concordancias</h1>
                <p class="subtitle">Buscador de palabras en contexto</p>
            </div>
            <% } %>
            <div class="containerr">
                <% if (projectId == null && request.getParameter("embed") == null) { %>
                <div id="tabs">
                    <ul>
                        <li><a href="#tabs-1">Seleccionar corpus</a></li>
                        <li><a href="#tabs-2">Búsqueda de concordancias</a></li>
                    </ul>
                    <div id="tabs-1">
                        <div class="row">
                            <div class="col-md-6">
                                <select class="corpus-select">
                                    <optgroup>
                                        <option value="null">Selecciona un proyecto</option>
                                    </optgroup>
                                    <% if (!"public".equals(user.get("login"))) { %>
                                    <optgroup class="own-projects" label="Mis Proyectos">
                                    </optgroup>
                                    <% } %>
                                    <optgroup class="public-projects" label="Proyectos públicos">
                                    </optgroup>
                                </select>
                            </div>
                            <div class="col-md-6">
                                Descripción del proyecto:
                                <div class="well project-description">Selecciona un proyecto</div>
                            </div>
                        </div>
                    </div>
                    <div id="tabs-2">
                        <% }%>
                        <div class="corpus-info alert-default block">
                            <strong>Corpus seleccionado:</strong> <span class="corpus-name">No se ha seleccionado ningún corpus</span>
                        </div>
                        <div class="metadatos" style="display:none">
                            <a href="#" class="mostrar-metadatos">Metadatos a visualizar <span class="glyphicon glyphicon-eye-open"></span></a>
                            <span class="elegir-metadatos" style="display:none"></span>
                            <a href="#" class="agregar-filtro">Agregar filtro <span class="glyphicon glyphicon-plus"></span></a>
                        </div>
                        <div class="metadatos container-filtros">
                            
                        </div>
                        <div class="clearfix"></div>
                        <div class="loading" style="display:none"></div>
                        <form class="formconcor form-horizontal" style="display:none">
                            <input type="hidden" name="project_id" value="<%= request.getParameter("pid")%>"/>
                            <div class="form-group">
                                <label for="query" class="col-sm-5 control-label">Petición de búsqueda</label>
                                <div class="col-sm-4">
                                    <input type="text" class="form-control" name="query" placeholder="Búsqueda"/>
                                </div>
                                <div class="col-sm-1">
                                    <a href="#" class="show-help blue"><span class="glyphicon glyphicon-question-sign"></span> Ayuda</a>
                                </div>
                                
                            </div>
                            <div class="form-group" id="ayuda" style="display:none">
                                    <p>La petición de búsqueda puede ser una palabra o una frase (varias palabras).
                                    Cada palabra de la petición puede tener un modificador diferente.</p>
                                    <table class="table table-striped">
                                        <tr>
                                            <th>Modificador</th>
                                            <th>Descripción</th>
                                            <th>Ejemplo petición</th>
                                            <th>Ejemplo resultados</th>
                                        </tr>
                                        <tr>
                                            <td>Sin modificador</td>
                                            <td title="(sensible a acentos y mayúsculas)">Forma concreta</td>
                                            <td>Avión</td>
                                            <td>Avión</td>
                                        </tr>
                                        <tr>
                                            <td>Corchetes</td>
                                            <td>Buscar lema</td>
                                            <td>[vivir]</td>
                                            <td>viví, vive, vivió</td>
                                        </tr>
                                        <tr>
                                            <td>Signos menor y mayor qué</td>
                                            <td>Buscar <a target="_new" title="Estándar EAGLES" class="blue" href="http://nlp.lsi.upc.edu/freeling-old/doc/tagsets/tagset-es.html">etiqueta POS</a></td>
                                            <td>&lt;VMIS3S0&gt;</td>
                                            <td>dijo, habló, tomó</td>
                                        </tr>
                                        <tr>
                                            <td>Asterisco</td>
                                            <td>Comodín, varias letras</td>
                                            <td>*ito</td>
                                            <td>chiquito, banquito, chorrito</td>
                                        </tr>
                                        <tr>
                                            <td>Asterisco</td>
                                            <td>Comodín, una palabra</td>
                                            <td>de * y *</td>
                                            <td>de carne y hueso, de oro y plata</td>
                                        </tr>
                                        <tr>
                                            <td>Signo de interrogación</td>
                                            <td>Comodín, una letra</td>
                                            <td>p?lo</td>
                                            <td>palo, pelo, polo</td>
                                        </tr>
                                        <tr>
                                            <td>Llaves</td>
                                            <td>Buscar en una distancia de hasta X palabras</td>
                                            <td>se {2} [definir] como</td>
                                            <td>se puede definir como, se define como, se puede encontrar definido como</td>
                                        </tr>
                                    </table>
                            </div>
                            <div class="form-group">                  
                                <label for="window" class="col-sm-5 control-label">Ventana</label>
                                <div class="col-sm-4">
                                    <input type="text" class="spinner" value="10" name="window" placeholder="Ventana">
                                </div>
                            </div>
                            <div class="form-group">                  
                                <label for="window" class="col-sm-5 control-label">Ordenamiento</label>
                                <div class="col-sm-4" id="sortSelects">
                                </div>
                                <a href="#" class="agregar-sort">Agregar ordenamiento <span class="glyphicon glyphicon-plus"></span></a>
                            </div>
                            <div class="form-group"> 
                                <div class="col-sm-5"></div>
                                <div class="col-sm-5">
                                    <a class="search btn btn-default">Buscar</a>
                                </div>
                            </div>
                        </form>
                        <div class="row" id="graficas">
                        </div>
                        <p style="display:none" class="result-none alert alert-warning">Esta búsqueda no generó resultados.</p>
                        <p style="display:none" class="result-error alert alert-danger">Ocurrió un error. Intente más tarde.</p>
                        <div class="botones row" style="display:none">
                            <div class="col-md-8">                             
                                <a href="#" class="btn btn-default result-download"><span class="glyphicon glyphicon-download-alt"></span> CSV</a>
                                <a href="#" class="btn btn-default result-download-excel"><span class="glyphicon glyphicon-download-alt"></span> Excel</a>                               
                                <a href="#" class="btn btn-default result-pos-highlight"><span class="glyphicon glyphicon-asterisk"></span> POS</a>
                                <a href="#" class="btn btn-default show-graphs" style="display:none"><span class="glyphicon glyphicon-stats"></span> Gráfico</a>
                                <br/>
                                Descarga:
                                <input type="checkbox" name="result-download-withlem"/>Con Lemas
                                <input type="checkbox" name="result-download-withpos"/>Con POS
                            </div>
                            <div class="col-md-4">
                                <p class="text-right">Se encontraron <span class="result-count"></span> resultados</span>
                            </div>
                        </div>
                        <table class="table striped" width="100%">
                            <thead class="result-header" style="display:none">
                                <tr>
                                    <th width="43%" class="izq">Izquierda</th>
                                    <th width="13%" class="kwic">Palabra</th>
                                    <th width="43%" class="der">Derecha</th>
                                </tr>
                            </thead>
                            <tbody class="result-body">
                            </tbody>
                        </table>
                        <div class="btn-block-container center-block text-center">
                            <button class="show-more btn btn-default" style="display:none">Mostrar más</button>                        
                        </div> 
                        <% if (projectId == null && request.getParameter("embed") == null) { %>
                    </div>
                </div>
            </div>
            <% }%>
    </body>
</html>
