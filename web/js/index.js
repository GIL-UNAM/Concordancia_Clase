/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

Concordancias = {
    results: [],
    last: 0,
    projectData: {},
    fields: [],
    fieldsSelected: [],
    posHighlight: false
};

function hideResults() {
    $(".result-body").empty();
    $(".result-error").hide();
    $(".result-none").hide();
    $(".result-header").hide();
    $(".botones").hide();
    $(".show-more").hide();
}

function addFilter(){
    html = '<div class="filter-box">';
    html += '<div class="pull-right"><a href="#" class="remove-filter"><span class="glyphicon glyphicon-remove"></span></a></div>';
    for(name in Concordancias.fields){
        if(name !== 'id'){
            html += '<div class="filter-field">';
            html += '<strong><span class="field-name">' + name + "</span></strong></br>";
            html += '<select name="'+name+'">';
            html += '<option value="">---</option>';
            for(value in Concordancias.fields[name]){
                html += '<option value="'+Concordancias.fields[name][value]+'">'+Concordancias.fields[name][value]+"</option>";
            }
            html += "</div>";
            html += "</select>";
        }
        
    }
    html += '</div>';
    $(".container-filtros").append(html).children().last().find(".remove-filter").click(function(e){
        $(e.target).parents(".filter-box").remove();
    });
}

function getFilters(){
    var filters = [];
    $(".filter-box").each(function(ind, box){
        var filter = [];
        $(box).find("select").each(function(ind, select){
            var campo = $(select).attr("name");
            var valor = $(select).val();
            if (valor!==""){
                filter.push([campo,valor]);
            }
        });
        filters.push(filter);
    });
    return filters;
}

function disableSearch(msj){
    if(!msj){
        msj = "Trabajando...";
    }
    $(".search").text(msj).attr("disabled", "disabled"); 
}

function enableSearch(){
    $(".search").text("Buscar").removeAttr("disabled");
}

function toggleGraphsButton(){
    if($(".elegir-metadatos input:checked").size()===0){
        $("a.show-graphs").hide();
    }else{
        $("a.show-graphs").show();
    }
}

function search() {  
    hideResults();
    project_id = $('input[name="project_id"]').val();
    if (project_id === "null") {
        $(".result-error").text("No se ha seleccionado ningún corpus").show();
        return;
    }
    query = $('input[name="query"]').val();
    if (!query) {
        $(".result-error").text("No has ingresado una petición").show();
        return;
    }
    sortFields = getSortFields();
    if(sortFields === false){
        return;
    }
    $(".loading").show();
    disableSearch();
    var data = {
        query: $('input[name="query"]').val(),
        window: $('input[name="window"]').val(),
        sort: sortFields,
        project_id: project_id,
        fields: Concordancias.fieldsSelected,
        filters: getFilters(),
        format: "json"
    };
    $.ajax({
        url: 'search',
        type: 'POST',
        data: JSON.stringify(data),
        dataType: 'json',
        success: function (data) {
            if(!data.error){
                Concordancias.resultsFields = data.fields;
                Concordancias.results = data.results;
                Concordancias.last = 0;
                showResults();
                $(".loading").hide();
                enableSearch();
                toggleGraphsButton();
            }else{
                $(".loading").hide();
                $(".result-error").text(data.error).show();
                enableSearch();
            }
        },
        error: function (data) {
            $(".loading").hide();
            $(".result-error").text("Error interno del servidor").show();
            enableSearch();
        }
    });
}

function prepareGraphs(){
    $("#graficas").empty().hide();
    Concordancias.plots = [];
    for(field in Concordancias.resultsFields){
        var chartId = "chart_" + field;
        var data = [];
        for(value in Concordancias.resultsFields[field]){
            data.push([value, Concordancias.resultsFields[field][value]]);
        }
        $("#graficas").append('<div class="chart col-md-3" id="'+chartId+'"></div>');
        var plotOptions = {
            seriesDefaults:{ renderer:$.jqplot.PieRenderer, trendline:{ show: true } },
            legend:{ show: true }    
        };
        Concordancias.plots.push({
            plotOptions: plotOptions,
            series: [data],
            chartId: chartId
        });
    }
}

function showResults() {
    prepareGraphs();
    $("span.result-count").text(Concordancias.results.length);
    if (Concordancias.results.length == 0) {
        $(".result-none").show();
    } else {
        $(".show-more").show();
        $(".botones").show();
        if(Concordancias.last==0){
            var header = "<tr>";
            header += '<th width="43%" class="izq">Izquierda</th>';
            header += '<th width="13%" class="kwic">Palabra</th>';
            header += '<th width="43%" class="der">Derecha</th>';
            for(i in Concordancias.fieldsSelected){
                field = Concordancias.fieldsSelected[i];
                header += "<th>" + field + "</th>";
            }
            header += "</tr>";
            $(".result-header").empty().html(header);
        }
        var start = Concordancias.last;
        for (i = start; i < start + 20; i++) {
            if (i == Concordancias.results.length) {
                $(".show-more").hide();
                break;
            }
            var conc = Concordancias.results[i];
            var fila = "<tr>";
            fila += "<td class=\"izq\">" + splitConc(conc.izq, conc.izqPOS) + "</td>";
            fila += "<td class=\"kwic\">" + splitConc(conc.kwic, conc.kwicPOS) + "</td>";
            fila += "<td class=\"der\">" + splitConc(conc.der, conc.derPOS) + "</td>";
            for(j in Concordancias.fieldsSelected){
                field = Concordancias.fieldsSelected[j];
                fila += "<td>" + conc[field] + "</td>";
            }
            fila += "</tr>";
            $(".result-body").append(fila);
            $(".result-header").show();
            Concordancias.last = i + 1;
            posHighlight();
        }
    }
}

function splitConc(concField, titleField){
    var splitted = concField.split(" ");
    var splittedTitle = titleField.split(" ");
    var html = "";
    for(w in splitted){
        word = splitted[w];
        wordTitle = splittedTitle[w];
        posClass = wordTitle ? wordTitle.charAt(0) : "?";
        html += "<span class='pos-"+posClass+"' title='"+wordTitle+"'>"+word+"</span> ";
    }
    return html;
}

function updateFieldsSelected(){
   Concordancias.fieldsSelected = $.map($(".metadata-checkbox:checked"), function(o){return $(o).val()});
}

function validateSortField(sortField){
    if(sortField !== "" && sortField !== "izq" && sortField !== "kwic" && sortField !== "der"){
        if(Concordancias.fieldsSelected.indexOf(sortField) === -1){
            $(".result-error").text("Selecciona el campo '"+ sortField + "' en los metadatos a visualizar").show();
            return false;
        }
    }
    return true;
}

function getSortFields(){
    sortFields = [];
    ok = true;
    $('select.sort').each(function(ind, ele){
        sortField = $(this).val();
        if(!validateSortField(sortField)){
            ok = false;
        }else{
            sortFields.push(sortField);
        }
    });
    return ok ? sortFields : false;
}

function downloadResults(format) {
    var buttonName = "a.result-download";
    if(format==="excel"){
        buttonName += "-excel";
    }
    sortFields = getSortFields();
    if(sortFields === false){
        return;
    }
    var data = {
        query: $('input[name="query"]').val(),
        window: $('input[name="window"]').val(),
        project_id: project_id,
        sort: sortFields,
        fields: Concordancias.fieldsSelected,
        filters: getFilters(),
        format: format
    };
    if($('input[name="result-download-withlem"]').prop("checked")){
        data.withLem = true;
    }
    if($('input[name="result-download-withpos"]').prop("checked")){
        data.withPOS = true;
    }
    $(buttonName).attr("disabled", "disabled");
    $.ajax({
        url: 'search',
        type: 'POST',
        data: JSON.stringify(data),
        success: function (data) {
            $(buttonName).removeAttr("disabled");
            if(format==='csv'){
                var url = "DownloadFile?fname=" + data;
            }else if(format==='excel'){
                var url = "DownloadExcelFile?fname=" + data;
            }
            window.location = url;
        }
    });
}

function generateSortSelect(){
    html = "";
    html += '<div class="sort-select-panel row">';
    html += '<div class="col-sm-10">';
    html += '<select class="form-control sort">';
    html +=     '<optgroup>';
    html +=         '<option value="">Bibliográfico</option>';
    html +=         '<option value="kwic">Alfabético - Petición</option>';
    html +=         '<option value="izq">Alfabético - Palabra a la izquierda</option>';
    html +=         '<option value="der">Alfabético - Palabra a la derecha</option>';
    html +=     '</optgroup>';
    html +=     '<optgroup class="sort_group_metadata">';
    for(i in Concordancias.fields){
        html +=     '<option value="'+i+'">'+i+'</option>';
    }
    html +=     '</optgroup>';
    html += '</select>';
    html += '</div>';
    html += '<div class="col-sm-2">';
    html += '<a href="#" class="remove-filter"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>';
    html += '</div>';
    html += '</div>';
    $("#sortSelects").append(html).children().last().find(".remove-filter").click(function(){
        $(this).parents(".sort-select-panel").remove();
        return false;
    });
}

function getMetadataFields(projectId){
    disableSearch("Inicializando");
    $(".loading").show();
    return $.ajax({
        url: 'FieldDictionary',
        data: {project_id: projectId},
        success: function(data){
            Concordancias.fields = data;
            $(".elegir-metadatos").empty();
            $("#sortSelects").empty();
            generateSortSelect();
            for(i in data){
                $(".elegir-metadatos").append('<input class="metadata-checkbox" type="checkbox" value="'+i+'"/>'+i+" ");
            }
            $(".container-filtros").empty();
            $(".metadata-checkbox").change(updateFieldsSelected);
            $(".loading").hide();
            enableSearch();
        },
        error: function () {
            $(".result-error").text("Ocurrió un error, intente más tarde.").show();
            $(".loading").hide();
        }
    });
}

function selectCorpus(pid){
    $(".corpus-name").text(Concordancias.projectData[pid].name);
    getMetadataFields(pid).done(function(){
        //Mostrar controles de los metadatos si el proyecto tiene metadatos
        if(Object.keys(Concordancias.fields).length > 0){
            $(".metadatos").show();
        }else{
            $(".metadatos").hide();
        }
    });
    $(".formconcor").show();
}

function getParam(name){
   if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search))
      return decodeURIComponent(name[1]);
}

function posHighlight(){
    if(Concordancias.posHighlight){
        $(".result-body").find("span").addClass("pos");
    }else{
        $(".result-body").find("span").removeClass("pos");
    }
}

$(document).ready(function () {
    $(".search").click(search);
    $("input").keypress(function (e) {
        if (e.keyCode == 13) {
            search();
        }
    });
    $("#tabs").tabs();
    $(".spinner").spinner();
    $.getJSON("projects", function (data) {
        for (i in data) {
            var selector = data[i].access == 'public' ? "optgroup.public-projects" : "optgroup.own-projects";
            $(selector).append('<option value="' + data[i].id + '">' + data[i].name + '</option>');
            Concordancias.projectData[data[i].id] = data[i];
        }
        $(".corpus-select").selectmenu({
            change: function (event, data) {
                $("input[name='project_id']").val(data.item.value);
                var description = Concordancias.projectData[data.item.value].description || "Sin descripción disponible";
                $(".project-description").html(description);
                hideResults();
                $("input[name='query']").val('');
                selectCorpus(data.item.value);
            }
        }).trigger("change");
        //Si en GET viene una query buscarla inmediatamente
        var query=getParam("query");
        var pid=getParam("pid");
        if(query && pid){
            $("input[name='query']").val(query);
            selectCorpus(pid);
            search();
        }else if(pid){
            selectCorpus(pid);
        }
    });
    $(".show-more").click(function () {
        showResults();
        $("html, body").animate({scrollTop: $(document).height()}, 1000);
    });
    $("a.result-download").click(function(){
        downloadResults("csv")
    });
    $("a.result-download-excel").click(function(){
        downloadResults("excel");
    });
    $("a.mostrar-metadatos").click(function(){
        $(".elegir-metadatos").toggle();
    });
    $("a.agregar-filtro").click(addFilter);
    $("a.show-help").click(function(){
        $("#ayuda").toggle("fast");
    })
    $("a.show-graphs").click(function(){
        $("#graficas").toggle("fast");
        for(plot in Concordancias.plots){
            plot = Concordancias.plots[plot];
            $.jqplot(plot.chartId, plot.series, plot.plotOptions).replot();
        }
    });
    $("a.result-pos-highlight").click(function(e){
        Concordancias.posHighlight = !Concordancias.posHighlight;
        posHighlight();
        return false;
    });
    $("a.agregar-sort").click(function(){
        generateSortSelect();
    });
})

