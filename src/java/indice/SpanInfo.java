/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indice;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author JSolorzanoS
 */
public class SpanInfo {
    private int start;
    private int end;
    private int startKwic;
    private int endKwic;
    private int doc;
    private final SortedMap<Integer, String> entriesForConcordance = new TreeMap<>();
    private final SortedMap<Integer, String> entriesForConcordancePOS = new TreeMap<>();
    private final SortedMap<Integer, String> entriesForConcordanceLem = new TreeMap<>();

    public SpanInfo(int start, int end, int startKwic, int endKwic, int doc) {
        this.start = start;
        this.end = end;
        this.startKwic = startKwic;
        this.endKwic = endKwic;
        this.doc = doc;
    }

    public void setEntriesForConcordance(int i, String term, String field) {
        if("content".equals(field)){
            this.entriesForConcordance.put(i, term);
        }
        else if("content_tags".equals(field)){
            this.entriesForConcordancePOS.put(i, term);
        }else if("content_lemmas".equals(field)){
            this.entriesForConcordanceLem.put(i, term);
        }
    }
    
     public Concordancia buildConcordance(IndexReader reader, String[] metadataFields, Map<String, 
             Map<String, Integer>> estadisticasPorCampo, String[] sortFields) throws IOException{
        StringBuilder sbIzq = new StringBuilder();
        StringBuilder sbIzqPOS = new StringBuilder();
        StringBuilder sbIzqLem = new StringBuilder();
        StringBuilder kwic = new StringBuilder();
        StringBuilder kwicPOS = new StringBuilder();
        StringBuilder kwicLem = new StringBuilder();
        StringBuilder sbDer = new StringBuilder();
        StringBuilder sbDerPOS = new StringBuilder();
        StringBuilder sbDerLem = new StringBuilder();
        for (Integer i : entriesForConcordance.keySet()) {
            String s = entriesForConcordance.get(i);
            String sPOS = entriesForConcordancePOS.get(i);
            String sLem = entriesForConcordanceLem.get(i);
            /* Si la posición es antes de la posición en que empieza el match,
            es la parte izquierda de la concordancia. */
            if (i < startKwic) {
                sbIzq.append(s);
                sbIzqPOS.append(sPOS);
                sbIzqLem.append(sLem);
                sbIzq.append(" ");
                sbIzqPOS.append(" ");
                sbIzqLem.append(" ");
            /* Cuando la posición es entre el inicio y el final del match, es la
                palabra o frase buscada (KWIC) */
            } else if (i >= startKwic && i < endKwic) {
                kwic.append(s);
                kwicPOS.append(sPOS);
                kwicLem.append(sLem);
                kwic.append(" ");
                kwicPOS.append(" ");
                kwicLem.append(" ");
            /* De otra manera, es la parte derecha de la concordancia */
            } else {
                sbDer.append(s);
                sbDerPOS.append(sPOS);
                sbDerLem.append(sLem);
                sbDer.append(" ");
                sbDerPOS.append(" ");
                sbDerLem.append(" ");
            }
        }
        Concordancia conc = new Concordancia(sbIzq.toString(), sbIzqPOS.toString(), sbIzqLem.toString(), kwic.toString(), kwicPOS.toString(), kwicLem.toString(),
            sbDer.toString(), sbDerPOS.toString(), sbDerLem.toString());
        for (String field : metadataFields) {
            String value = reader.document(doc).get(field);
            conc.getMetadatos().put(field, value);
            /*Se contabiliza en el diccionario de frecuencias*/
            if(estadisticasPorCampo != null){
                Map<String, Integer> estadisticasCampo = estadisticasPorCampo.get(field);
                estadisticasCampo.put(value, estadisticasCampo.getOrDefault(value,0)+1);
            }
        }
        if(sortFields != null){
            conc.setSortFields(sortFields);
        }
        
        return conc;
    }
     
    public String toString(){
        String s = String.format("[start=%s], [end=%s], [kwicStart=%s], [kwicEnd=%s], [doc=%s]\n", 
                this.start, this.end, this.startKwic, this.endKwic, this.doc);
        for(Integer position : this.entriesForConcordance.keySet()){
            s += String.format("%s: %s ", position, this.entriesForConcordance.get(position));
        }
        return s;
    }
    
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStartKwic() {
        return startKwic;
    }

    public void setStartKwic(int startKwic) {
        this.startKwic = startKwic;
    }

    public int getEndKwic() {
        return endKwic;
    }

    public void setEndKwic(int endKwic) {
        this.endKwic = endKwic;
    }

    public int getDoc() {
        return doc;
    }

    public void setDoc(int doc) {
        this.doc = doc;
    }
    
}
