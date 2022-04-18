/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import helpers.FilterExpression;
import helpers.ListOfAndFilters;
import indice.Concordancia;
import indice.Indice;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author JSolorzanoS
 */
@WebServlet(name = "Search", urlPatterns = {"/search"})
public class Search extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        //Obtener parametros JSON
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
        String json = br.readLine();
        JSONObject paramsObj = (JSONObject) JSONValue.parse(json);
        
        //Parámetros básicos
        int projectId = Integer.parseInt((String)paramsObj.get("project_id"));
        String query = (String)paramsObj.get("query");
        String sWindow = (String)paramsObj.get("window");
        JSONArray jsonSortFields = (JSONArray) paramsObj.get("sort");
        String[] sortFields = new String[jsonSortFields.size()];
        for(int i=0;i<sortFields.length;i++){
            sortFields[i] = (String) jsonSortFields.get(i);
        }
        String format = (String)paramsObj.get("format");
        int iWindow = 10;
        if (sWindow != null && !"".equals(sWindow)) {
            try {
                iWindow = Integer.parseInt(sWindow);
            } catch (NumberFormatException ex) {
            }
        }
        boolean withLem = paramsObj.get("withLem") != null;
        boolean withPOS = paramsObj.get("withPOS") != null;
        
        //Filtros
        List<ListOfAndFilters> filters = new ArrayList<>();
        JSONArray listaDeFiltros = (JSONArray) paramsObj.get("filters");
        for(Object oFiltro : listaDeFiltros){
            ListOfAndFilters lFiltro = new ListOfAndFilters();
            JSONArray filtro = (JSONArray) oFiltro;
            for(Object oExpresion : filtro){
                JSONArray expresion = (JSONArray) oExpresion;
                String campo = (String) expresion.get(0);
                String valor = (String) expresion.get(1);
                if(!"".equals(valor)){
                    System.out.println(campo + " " + valor);
                    FilterExpression filtExp = new FilterExpression(campo,valor);
                    lFiltro.add(filtExp);
                }
            }
            if(!lFiltro.isEmpty()){
                filters.add(lFiltro);
            }
        }
        
        //Metadatos visibles
        JSONArray jFields = (JSONArray) paramsObj.get("fields");
        String[] fields = new String[jFields.size()];
        for(int i = 0; i<jFields.size(); i++){
            String field = (String) jFields.get(i);
            fields[i] = field;
        }
        
        //Búsqueda
        response.setContentType("application/json;charset=utf-8");
        PrintWriter pw = response.getWriter();
        try {
            Indice ind = new Indice(request, projectId);           
            Concordancia[] concs = ind.getConcordances(query, iWindow, filters, fields, sortFields);         
            if (format.equals("json")) { //Para mostrar en navegador (json)               
                JSONArray jarr = new JSONArray();
                for (Concordancia conc : concs) {
                    JSONObject jsonConc = new JSONObject();
                    jsonConc.put("izq", conc.getIzquierda());
                    jsonConc.put("kwic", conc.getKwic());
                    jsonConc.put("der", conc.getDerecha());
                    jsonConc.put("izqPOS", conc.getIzquierdaPOS());
                    jsonConc.put("kwicPOS", conc.getKwicPOS());
                    jsonConc.put("derPOS", conc.getDerechaPOS());
                    for(String field:fields){
                        jsonConc.put(field, conc.getMetadatos().get(field));
                    }
                    jarr.add(jsonConc);
                }
                
                //Estadisticas por campo
                JSONObject estadisticas = new JSONObject();
                for (String campo: ind.getEstadisticasPorCampo().keySet()){
                    estadisticas.put(campo, ind.getEstadisticasPorCampo().get(campo));
                }
                
                JSONObject result = new JSONObject();
                result.put("results", jarr);
                result.put("fields", estadisticas);
                
                String res = JSONObject.toJSONString(result);
                pw.print(res);
            } else if(format.equals("csv")) { //Para generar CSV
                response.setContentType("text/plain");
                
                List<String> csvData = new ArrayList<String>();
                String header = "Derecha,Palabra,Izquierda";
                for(String field:fields){
                    header += "," + field;
                }
                csvData.add(header);
                String row = "";
                for (Concordancia conc : concs) {
                    row += quoteEscape(conc.getIzquierda(withLem, withPOS)) + ","
                            + quoteEscape(conc.getKwic(withLem, withPOS)) + "," + quoteEscape(conc.getDerecha(withLem, withPOS));
                    for(String field:fields){
                        row += "," + conc.getMetadatos().get(field);
                    }
                    csvData.add(row);
                    row = "";
                }
                List<String> lines = csvData;
                Path file = File.createTempFile("concordanciasDownload", ".tmp").toPath(); 
                Files.write(file, lines, Charset.forName("UTF-8"));
                pw.print(file.getFileName());
            } else if(format.equals("excel")){ //Para generar archivo excel
                response.setContentType("text/plain");
                
                Workbook wb = new XSSFWorkbook();
                Sheet sheet = wb.createSheet("Concordancias");
                
                CellStyle boldStyle = wb.createCellStyle();
                Font font = wb.createFont();
                font.setBoldweight(Font.BOLDWEIGHT_BOLD);
                boldStyle.setFont(font);
                
                CellStyle headerStyle = wb.createCellStyle();
                headerStyle.setFillForegroundColor(IndexedColors.YELLOW.index);
                headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
                headerStyle.setFont(font);
                
                Row headerRow = sheet.createRow((short) 0);
                String[] headerFields = new String[]{"Izquierda", "Palabra", "Derecha"};
                for(int i=0;i<headerFields.length;i++){
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerFields[i]);
                    cell.setCellStyle(headerStyle);
                    if(i==0){
                        CellUtil.setAlignment(cell, wb, CellStyle.ALIGN_RIGHT);
                    }
                }
                for(int i=0;i<fields.length;i++){
                    Cell cell = headerRow.createCell(3+i);
                    cell.setCellValue(fields[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                Row row;
                for (int i=0;i<concs.length;i++) {
                    row = sheet.createRow((short) 1+i);
                    
                    Cell cellLeft = row.createCell(0);
                    cellLeft.setCellValue(concs[i].getIzquierda(withLem, withPOS));
                    CellUtil.setAlignment(cellLeft, wb, CellStyle.ALIGN_RIGHT);
                    
                    Cell cellCenter = row.createCell(1);
                    cellCenter.setCellValue(concs[i].getKwic(withLem, withPOS));
                    cellCenter.setCellStyle(boldStyle);
                    CellUtil.setAlignment(cellCenter, wb, CellStyle.ALIGN_CENTER);
                    
                    row.createCell(2).setCellValue(concs[i].getDerecha(withLem, withPOS));
                    for(int j=0;j<fields.length;j++){
                        row.createCell(3+j).setCellValue(concs[i].getMetadatos().get(fields[j]));
                    }
                }
                
                sheet.autoSizeColumn((short)0);
                sheet.autoSizeColumn((short)1);
                sheet.autoSizeColumn((short)2);
                
                File file = File.createTempFile("concordanciasDownload", ".tmp");
                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    wb.write(fileOut);
                }
                pw.print(file.getName());
            }
        } catch(Exception ex){
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            pw.print("{\"error\": \"" + ex.getMessage() + "\"}");
        }

    }

    private String quoteEscape(String s) {
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
