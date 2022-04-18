/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import indice.Indice;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author JSolorzanoS
 */
@WebServlet(name = "FieldDictionary", urlPatterns = {"/FieldDictionary"})
public class FieldDictionary extends HttpServlet {

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
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        int proyectoId = Integer.parseInt(request.getParameter("project_id"));
        PrintWriter out = response.getWriter();
        try {
            Indice indice = new Indice(request, proyectoId);
            IndexReader indexReader = indice.getIndexSearcher().getIndexReader();
            Fields fields = MultiFields.getFields(indexReader);
            Iterator<String> fieldIterator = fields.iterator();
            String fieldName = null;
            JSONObject result = new JSONObject();
            while(fieldIterator.hasNext()) {
                fieldName = fieldIterator.next();
                if(fieldName.startsWith("content") || fieldName.equals("id")){
                    continue;
                }
                JSONArray values = new JSONArray();
                result.put(fieldName, values);
                LuceneDictionary ld = new LuceneDictionary(indexReader, fieldName);
                BytesRefIterator iterator = ld.getWordsIterator();
                BytesRef byteRef = null;
                while ((byteRef = iterator.next()) != null) {
                    String term = byteRef.utf8ToString();
                    values.add(term);
                }
            }
            out.print(result.toJSONString());
        } catch (Exception ex) {
            Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServletException(ex.getMessage());
        }
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
