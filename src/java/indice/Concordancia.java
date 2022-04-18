/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indice;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author JSolorzanoS
 */
public class Concordancia implements Comparable<Concordancia> {
    private String izquierda;
    private String izquierdaPOS;
    private String izquierdaLem;
    private String kwic;
    private String kwicPOS;
    private String kwicLem;
    private String derecha;
    private String derechaPOS;
    private String derechaLem;
    private String[] sortFields;
    private Map<String, String> metadatos = new HashMap<>();

    public Concordancia(String izquierda, String kwic, String derecha) {
        this.izquierda = izquierda;
        this.kwic = kwic;
        this.derecha = derecha;
    }

    public Concordancia(String izquierda, String izquierdaPOS, String izquierdaLem, 
            String kwic, String kwicPOS, String kwicLem, String derecha, String derechaPOS, String derechaLem) {
        this.izquierda = izquierda;
        this.izquierdaPOS = izquierdaPOS;
        this.izquierdaLem = izquierdaLem;
        this.kwic = kwic;
        this.kwicPOS = kwicPOS;
        this.kwicLem = kwicLem;
        this.derecha = derecha;
        this.derechaPOS = derechaPOS;
        this.derechaLem = derechaLem;
    }
    
    public String getKwic(boolean withLem, boolean withPOS){      
        if(!withLem && !withPOS){
            return this.kwic;
        }
        StringBuilder sb = new StringBuilder();
        String[] normal = this.kwic.split(" ");
        String[] lem = this.kwicLem.split(" ");
        String[] pos = this.kwicPOS.split(" ");
        for(int i=0;i<normal.length;i++){
            sb.append(normal[i]);
            if(withLem){
                sb.append("_");
                sb.append(lem[i]);
            }
            if(withPOS){
                sb.append("_");
                sb.append(pos[i]);
            }
            sb.append(" ");
        }
        return sb.toString();
    }
    
    public String getIzquierda(boolean withLem, boolean withPOS){
        if(!withLem && !withPOS){
            return this.izquierda;
        }
        StringBuilder sb = new StringBuilder();
        String[] normal = this.izquierda.split(" ");
        String[] lem = this.izquierdaLem.split(" ");
        String[] pos = this.izquierdaPOS.split(" ");
        for(int i=0;i<normal.length;i++){
            sb.append(normal[i]);
            if(withLem){
                sb.append("_");
                sb.append(lem[i]);
            }
            if(withPOS){
                sb.append("_");
                sb.append(pos[i]);
            }
            sb.append(" ");
        }
        return sb.toString();
    }
    
    public String getDerecha(boolean withLem, boolean withPOS){
        if(!withLem && !withPOS){
            return this.derecha;
        }
        StringBuilder sb = new StringBuilder();
        String[] normal = this.derecha.split(" ");
        String[] lem = this.derechaLem.split(" ");
        String[] pos = this.derechaPOS.split(" ");
        for(int i=0;i<normal.length;i++){
            sb.append(normal[i]);
            if(withLem){
                sb.append("_");
                sb.append(lem[i]);
            }
            if(withPOS){
                sb.append("_");
                sb.append(pos[i]);
            }
            sb.append(" ");
        }
        return sb.toString();
    }
    
    public String getIzquierda() {
        return izquierda;
    }

    public void setIzquierda(String izquierda) {
        this.izquierda = izquierda;
    }

    public String getKwic() {
        return kwic;
    }

    public void setKwic(String kwic) {
        this.kwic = kwic;
    }

    public String getDerecha() {
        return derecha;
    }

    public void setDerecha(String derecha) {
        this.derecha = derecha;
    }

    public String getIzquierdaPOS() {
        return izquierdaPOS;
    }

    public void setIzquierdaPOS(String izquierdaPOS) {
        this.izquierdaPOS = izquierdaPOS;
    }

    public String getKwicPOS() {
        return kwicPOS;
    }

    public void setKwicPOS(String kwicPOS) {
        this.kwicPOS = kwicPOS;
    }

    public String getDerechaPOS() {
        return derechaPOS;
    }

    public void setDerechaPOS(String derechaPOS) {
        this.derechaPOS = derechaPOS;
    }

    public Map<String, String> getMetadatos() {
        return metadatos;
    }

    public void setMetadatos(Map<String, String> metadatos) {
        this.metadatos = metadatos;
    }

    public String[] getSortFields() {
        return sortFields;
    }

    public void setSortFields(String[] sortFields) {
        this.sortFields = sortFields;
    }
    
    /**
     * Determina el valor por el cual ha de ser comparada la concordancia para un ordenamiento.
     * 
     * Se determina por medio del argumento <i>sortField</i> cuyos poisbles valores son
     * <ul>
     * <li><b>izq</b> Ordenar por la palabra a la izquierda
     * <li><b>kwic</b> Ordenar por la petición
     * <li><b>der</b> Ordenar por la palabra a la derecha
     * <li>Más todos los nombres de los campos de los metadatos que se hayan solicitado en la búsqueda</li>
     * </ul>
     * 
     * @param sortField El campo del cual se quiere el valor para el ordenamiento
     * @throws RuntimeException Si el campo a comparar es inválido
     * @return El valor a ser comparado por la función compareTo
     */
    public String getSortFieldValue(String sortField){
        String valueToCompare = "";
        if(sortField.equals("kwic")){
            valueToCompare = this.kwic;
        }else if(sortField.equals("der")){
            // Si es por la palabra a la derecha, tomar la primera token alfanumérica del texto de la derecha.
            String[] splitted = this.derecha.trim().split(" ");
            for (String splitted_i : splitted) {
                valueToCompare = splitted_i;
                if(valueToCompare.length() > 1 || Character.isLetterOrDigit(valueToCompare.charAt(0))){
                    break;
                }
            }
        }else if(sortField.equals("izq")){
            // Si es por la palabra a la izquierda, tomar la última token alfanúmerica del texto de la izquierda
            String[] splitted = this.izquierda.trim().split(" ");
            for(int i = splitted.length - 1; i >= 0; i--){
                valueToCompare = splitted[i];
                if(valueToCompare.length() > 1 || Character.isLetterOrDigit(valueToCompare.charAt(0))){
                    break;
                }
            }
        }else if(this.metadatos.containsKey(sortField)){
            valueToCompare = this.metadatos.get(sortField);
        }else{
            throw new RuntimeException("Sort con campo inválido " + sortField);
        }
        /* Para que no aparezcan primero las que empiezan con mayúscula y 
        haya "dos" ordenamientos, convertir todo a minúsculas. Por razones
        similares, también quitar los acentos. */
        valueToCompare = valueToCompare.toLowerCase();
        valueToCompare = quitarAcentos(valueToCompare);
        return valueToCompare;
    }

    @Override
    public int compareTo(Concordancia o) {
        int comparison = 0;
        for (String sortField: this.sortFields){
            if("".equals(sortField)){
                continue;
            }
            String value1 = this.getSortFieldValue(sortField);
            String value2 = o.getSortFieldValue(sortField);
            comparison = value1.compareTo(value2);
            if(comparison != 0){
                break;
            }
        }
        return comparison;
    }
    
    // http://stackoverflow.com/questions/3322152/is-there-a-way-to-get-rid-of-accents-and-convert-a-whole-string-to-regular-lette
    private String quitarAcentos(String string){
        char[] out = new char[string.length()];
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        int j = 0;
        for (int i = 0, n = string.length(); i < n; ++i) {
            char c = string.charAt(i);
            if (c <= '\u007F') out[j++] = c;
        }
        return new String(out);
    }
    
    
       
}
