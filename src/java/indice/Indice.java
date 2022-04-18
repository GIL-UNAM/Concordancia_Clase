/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indice;

import conexion.OdooBridge;
import helpers.FilesUtilities;
import helpers.FilterExpression;
import helpers.ListOfAndFilters;
import helpers.SeparateTokenFiles;
import helpers.UnzipUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;

/**
 *
 * @author JSolorzanoS
 */
public class Indice {

    private Directory indexDirectory;
    private IndexSearcher indexSearcher;
    private Path indexPath;
    private String[] metadataFields;
    private String[][] metadataValues;
    private Map<String, String[]> metadataIndex;
    private Map<String, Map<String, Integer>> estadisticasPorCampo;
    private final int proyectoId;
    private final HttpServletRequest request;
    static private final String LUCENE_DIRECTORY = "/tmp/lucene_directory";

    
    public Indice(HttpServletRequest request, int proyectoId) throws Exception {
        this.request = request;
        this.proyectoId = proyectoId;
        resetDirectories();
        initSearcher(false);
    }
    
    /**
     * Permite especificar si ha de forzarse la creación del índice.
     * 
     * @param request
     * @param proyectoId
     * @param forceCreate Especifica si ha de forzarse la creación del índice,
     * aunque ya se hubiera indexado anteriormente
     */
    public Indice(HttpServletRequest request, int proyectoId, boolean forceCreate) throws Exception {
        this.request = request;
        this.proyectoId = proyectoId;
        resetDirectories();
        initSearcher(forceCreate);
    }

    /**
     * Inicializa los valores de los directorios donde se ha de localizar el índice.
     */
    private void resetDirectories() throws Exception {
        if(this.indexDirectory != null){
            indexDirectory.close();
            this.indexDirectory = null;
        }
        // gch String _parentIndexDirectory = System.getenv("LUCENE_DIRECTORY");
        String _parentIndexDirectory = LUCENE_DIRECTORY;
        Path parentIndexDirectory = Paths.get(_parentIndexDirectory, "project_indexes");
        if (!parentIndexDirectory.toFile().exists()) {
            try {
                Files.createDirectory(parentIndexDirectory);
            } catch (IOException ex) {
                throw new Exception("Error de I/O al crear el directorio para los índices " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        indexPath = Paths.get(parentIndexDirectory.toString(), "index_project_" + proyectoId);
        if (!indexPath.toFile().exists()) {
            try {
                Files.createDirectory(indexPath);
            } catch (IOException ex) {
                throw new Exception("Error de I/O al crear carpeta para el índice del proyecto " + this.proyectoId + ". " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        try {
            this.indexDirectory = new SimpleFSDirectory(indexPath.toFile());
        } catch (IOException ex) {
            throw new Exception("Error de I/O al abrir el directorio del índice del proyecto " + this.proyectoId + ". " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
    
    /**
     * Crea un searcher usando el directorio del índice del proyecto.
     *
     * Si aún no existe el índice, lo crea en este momento, descargando los documentos
     * del proyecto de GECO. También re-crea el índice si los documentos del
     * proyecto han cambiado desde la última indexación.
     * 
     * @param forceCreate True para que el índice sea recreado independientemente
     * de si ya existe o no
     */
    private void initSearcher(boolean forceCreate) throws Exception{
        boolean create = forceCreate;
        if(!forceCreate){
            try {
                this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
            } catch (IndexNotFoundException ex) {
                create = true;
            }

            /*Si la fecha del último indexado (conocida por la fecha de última modificación
            de la carpeta donde está el índice) es menor a la fecha de última modificación
            del proyecto, re-crear el índice */ 
            JSONObject project = OdooBridge.getProject(request, proyectoId);
            String lastUpdate = (String) project.get("last_update");
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(tz);
            Date dateLastModified = new Date(indexPath.toFile().lastModified());
            String lastIndex = df.format(dateLastModified);
            System.out.println("Last index: " + lastIndex);
            System.out.println("Last update: " + lastUpdate);
            if(lastIndex.compareTo(lastUpdate) < 0){
                create = true;
            }
        }
        if(create){
            try{
                Path pathToFiles = downloadAndPrepareProjectDocuments();
                createIndex(pathToFiles);
                resetDirectories();
                this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
            } catch (IOException ex) {
                throw new Exception("Error de I/O al instanciar un buscador en el índice del proyecto " 
                        + this.proyectoId + ". " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        
    }

    /* Función auxiliar para quitar las comillas de las columnas de los csv */
    private String stripQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace("\"\"", "\"");
        return s;
    }

    /**
     * Descarga los documentos del proyecto y los prepara para su indexación.
     *
     * Esta preparación implica parsear los documentos para que queden los tokens,
     * los lemmas y las etiquetas POS en archivos separados, además de que se
     * descargan los metadatos de los documentos del proyecto para indexarlos también.
     * 
     * @return Regresa la ruta donde son descomprimidos.
     */
    private Path downloadAndPrepareProjectDocuments() throws Exception {
        String datasBase64 = OdooBridge.downloadZip(request, new int[]{}, proyectoId, false);
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] zipBytes = decoder.decode(datasBase64);
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("toIndex_");
        } catch (IOException ex) {
            throw new Exception("Error de I/O al crear un directorio temporal para los documentos a indexar " 
                    + ex.getClass().getName() + " " + ex.getMessage());
        }
        try {
            UnzipUtility.unzip(zipBytes, tempDir);
        } catch (IOException ex) {
            throw new Exception("Error de I/O al crear descomprimir los archivos a indexar " 
                    + ex.getClass().getName() + " " + ex.getMessage());
        }
        System.out.println(tempDir);
        try {
            new SeparateTokenFiles(tempDir).process();
        } catch (IOException ex) {
            throw new Exception("Error de I/O al crear los archivos de tokens separados" 
                    + ex.getClass().getName() + " " + ex.getMessage());
        }
        String csv;
        try {
            csv = OdooBridge.getProjectMetadata(request, proyectoId, false);
        } catch (Exception ex) {
            throw new Exception("Error al hacer llamada al API de GECO - " 
                    + ex.getClass().getName() + " " + ex.getMessage());
        }
        String[] rows = csv.split("\n");
        metadataFields = rows[0].split(",");
        for (int i = 0; i < metadataFields.length; i++) {
            metadataFields[i] = stripQuotes(metadataFields[i]);
        }
        int nDocs = rows.length - 1;
        int nFields = metadataFields.length;
        metadataValues = new String[nDocs][nFields];
        metadataIndex = new HashMap<>();
        for (int i = 1; i < rows.length; i++) {
            String[] rowValues = rows[i].split(",");
            String id = "";
            for (int j = 0; j < nFields; j++) {
                String value = stripQuotes(rowValues[j]);
                String field = stripQuotes(metadataFields[j]);
                if (field.equals("id")) {
                    id = value;
                }
                metadataValues[i - 1][j] = value;
            }
            metadataIndex.put(id, metadataValues[i - 1]);
        }
        return tempDir;
    }
    
    /**
     * Indexa los documentos del proyecto.
     * 
     * @param pathToFiles La ruta al directorio temporal donde los archivos de GECO
     * fueron descargados y procesados (ver {@link #downloadAndPrepareProjectDocuments()}).
     */
    private void createIndex(Path pathToFiles) throws Exception {
        indexDirectory.close();
        resetDirectories();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43,
                new WhitespaceAnalyzer(Version.LUCENE_43));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(indexDirectory, config);
        try {
            // Tipo de campo que guarda las posiciones
            FieldType type = new FieldType();
            type.setStoreTermVectors(true);
            type.setStoreTermVectorOffsets(true);
            type.setStoreTermVectorPositions(true);
            type.setIndexed(true);
            type.setTokenized(true);

            Path pathTokens = Paths.get(pathToFiles.toString(), "tokens");
            File[] files = pathTokens.toFile().listFiles();
            for (File file : files) {
                Document doc = new Document();

                //Campos a guardar por cada archivo
                //--------------------------------
                //Metadatos
                System.out.println("Indexando " + file.getName());
                String id = file.getName().replace(".txt", "");
                String[] docMetadata = metadataIndex.get(id);
                for (int i = 0; i < metadataFields.length; i++) {
                    Field field = new StringField(metadataFields[i], docMetadata[i], Field.Store.YES);
                    doc.add(field);
                    System.out.println(metadataFields[i] + ": " + docMetadata[i]);
                }

                try {
                    //Normal
                    InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
                    Field text = new Field("content", reader, type);

                    //Lematizado
                    String fileLemmas = file.getAbsolutePath().replace("tokens", "lemmas");
                    reader = new InputStreamReader(new FileInputStream(fileLemmas), "UTF-8");
                    Field textLemmas = new Field("content_lemmas", reader, type);

                    //Etiquetas POS
                    String fileTags = file.getAbsolutePath().replace("tokens", "tags");
                    reader = new InputStreamReader(new FileInputStream(fileTags), "UTF-8");
                    Field textTags = new Field("content_tags", reader, type);

                    doc.add(text);
                    doc.add(textLemmas);
                    doc.add(textTags);
                    writer.addDocument(doc);

                } catch (FileNotFoundException ex) {
                    throw new Exception("Error al leer archivos de tokens para indexar campos. " 
                            + ex.getClass().getName() + ": " + ex.getMessage());
                } catch (UnsupportedEncodingException ex) {
                    throw new Exception("Error al establecer la codificación de los archivos de tokens para indexar campos. " 
                            + ex.getClass().getName() + ": " + ex.getMessage());
                } catch (IOException ex) {
                    throw new Exception("Error de I/O al escribir un documento al índice. "  + ex.getClass().getName() + ": " 
                            + ex.getMessage());
                }
            }
        }finally{
            try{
                writer.close();
            }catch(Exception ex){}
        }
        //Borrar archivos temporales
        FilesUtilities.deleteRecursive(pathToFiles.toFile());
    }

    /**
     * Devuelve las concordancias dada una serie de parámetros de búsqueda.
     * 
     * <p>La cadena de búsqueda puede estar formada de una varias palabras, las
     * cuales pueden estar modificadas por los siguientes símbolos.</p>
     * <ul>
     * <li> [] (entre corchetes). Para buscar por lema. </li>
     * <li> <> (entre picoparentesis). Para buscar por etiquetas POS. </li>
     * <li> * (asterisco). Comodín, reemplaza letras o una palabra entera. </li>
     * </ul>
     * <p>La lista de filtros limita el conjunto de documentos en los cuales
     * hacer la búsqueda solicitada. Cada elemento de la lista de filtros
     * es una serie de expresiones de la forma [campo, operador, valor] que han de
     * cumplirse simultáneamente (están unidas por AND). Los elementos de
     * dicha lista se unirán con un OR para determinar el conjunto final
     * de documentos permitidos</p>
     * <p>Cada concordancia puede además venir acompañada de ciertos
     * metadatos del documento del cual fue extraída. El parámetro de campos
     * de metadatos especifica qué metadatos han de incluirse en el resultado.</p>
     * 
     * @param queryTerm La cadena de búsqueda.
     * @param window El número de palabras a la izquierda y a la derecha.
     * @param filters Lista de filtros (que serán unidos con el operador OR) para
     * limitar los documentos de los cuales se muestran los resultados
     * @param metadataFields Lista de metadatos de los cuales obtener el valor 
     * para desplegar en los resultados.
     * @param sortFields El tipo de ordenamiento para mostrar los resultados, se espera
     * que sea <i>izq</i>, <i>der</i>, <i>kwic</i> o el nombre de
     * un campo de metadato. Puede mandarse cadena vacía en cuyo caso no se hará ningún
     * ordenamiento.
     * @return La lista de concordancias resultantes
     * @throws IOException Si no puede abrir algún directorio
     */
    public Concordancia[] getConcordances(String queryTerm, int window,
            List<ListOfAndFilters> filters, String[] metadataFields, String[] sortFields) throws IOException, BadQueryException {
        
        /*Limpiar la cadena compactando los espacios en blancos y quitando los
        que están al inicio y al final*/
        queryTerm = queryTerm.trim();
        queryTerm = queryTerm.replaceAll("\\s+", " ");

        /* Dividir por espacios la cadena de búsqueda y construir un arreglo
        de objetos SpanQuery lo suficientemente grande para instanciar uno
        por cada palabra de la petición de búsqueda */
        String[] queryWords = queryTerm.split(" ");
        SpanQuery[] queries = new SpanQuery[queryWords.length];
        
        /* Cola para saber cuánto mide cada hueco de los que vienen entre llaves */
        Queue<Integer> gaps = new LinkedList<>();
        
        /*Para validar*/
        boolean atLeastOneNonWilcard = false;

        /*Instanciar los objetos del arreglo de queries*/
        String campo, campoAnterior = null, campoMask = null;
        for(int i =0; i<queryWords.length; i++){
            String queryWord = queryWords[i];
            /*Por defecto el campo a buscar es el que contiene los tokens
            tal cual aparecen en el corpus */
            campo = "content"; 
            
            /* Si la palabra está entre corchetes significa que se debe buscar
            por lema, por lo tanto el campo a buscar cambia */
            if (queryWord.charAt(0) == '[' && queryWord.charAt(queryWord.length() - 1) == ']') {
                campo = "content_lemmas";
                queryWord = queryWord.substring(1, queryWord.length() - 1);
            }
            /* Si está entre picoparéntesis, se debe buscar por etiqueta POS */
            else if (queryWord.charAt(0) == '<' && queryWord.charAt(queryWord.length() - 1) == '>') {
                campo = "content_tags";
                queryWord = queryWord.substring(1, queryWord.length() - 1);
            }
             /* Si está entre llaves y es un número, es que puede haber ese número de huecos entre
            la palabra anterior y la siguiente. Añadir el número a una lista.
            */
            else if (queryWord.charAt(0) == '{' && queryWord.charAt(queryWord.length() - 1) == '}'){
                try{
                    gaps.add(Integer.parseInt(queryWord.substring(1, queryWord.length() - 1)));
                }catch(NumberFormatException ex){
                    throw new BadQueryException("Número inválido entre llaves");
                }
                continue;
            }           
            
            /*SpanQuery para esta palabra*/         
            SpanQuery sq;
            /*Término a buscar*/
            Term term = new Term(campo, queryWord);
            
            if(!queryWord.equals("*") && !queryWord.equals("?")){
                atLeastOneNonWilcard = true;
            }
            
            /*Si no hay comodines, generar una SpanTermQuery*/
            if(!queryWord.contains("*") && !queryWord.contains("?")){
                sq = new SpanTermQuery(term);
            }else{
                /* Si hay comodines (*, ?) la query debe ser una WildcardQuery
                en un wrapper que la convierta en SpanQuery*/
                WildcardQuery wq = new WildcardQuery(term);
                sq = new SpanMultiTermQueryWrapper<>(wq);
            }       
            
            /* Para multipalabra, en el caso de que los campos a buscar sean mezclados, es 
            decir que en la misma petición de búsqueda se está solicitando por ejemplo
            una palabra lema y una palabra token, entonces se debe wrappear en una
            FieldMaskingSpanQuery ya que las claúsulas del SpanNearQuery que
            se instanciará más abajo para buscar la frase completa no pueden
            referirse a campos diferentes. El campo que se usará como máscara
            será simplemente el que corresponda a la primer palabra de la petición.*/
            if(campoMask == null){
                campoMask = campo;
            }           
            if(queryWords.length > 1){
                queries[i] = new FieldMaskingSpanQuery(sq, campoMask);
            }else{
                queries[i] = sq;
            }
            campoAnterior = campo;
        }
        
        /*Al menos un término debe ser un no-comodín*/
        if(!atLeastOneNonWilcard){
            throw new BadQueryException("La consulta no puede estar conformada únicamente de comodines");
        }
              
        /* Referencia para la query final*/
        SpanQuery q;
        
        if(queries.length == 1){
            /* Si no es búsqueda multipalabra usar la SpanQuery ya instanciada, 
            que en este caso queda como primer y único elemento del arreglo de 
            querys */
            q = queries[0];
        }else{
            /* De otra manera, usar una SpanNearQuery, que sirve para buscar varias palabras. 
            Puede buscarlas con cierto número de huecos entre ellas (segundo parámetro, slop)
            y puede buscarlas en orden o en desorden (tercer parámetro true o false respectivamente).*/
            
            /* Si no hay huecos simplemente hacer la SpanNearQuery con todas las palabras y slop 0 */
            if (gaps.size() == 0){
                q = new SpanNearQuery(queries, 0, true);
            /*De otro modo hay que hacer un proceso para anidar varias SpanNearQuery con distintos slops*/
            }else{
                /*Validaciones*/
                boolean previousIsNull = false;
                for(int i=0;i<queries.length;i++){                  
                    if(queries[i] == null){
                        if(i == 0 || i == queries.length - 1){
                            throw new BadQueryException("No puede haber huecos al principio o al final de la consulta");
                        }
                        if(previousIsNull){
                            throw new BadQueryException("No puede haber dos huecos juntos");
                        }
                        previousIsNull = true;
                    }else{
                        previousIsNull = false;
                    }
                }
                
                /*Algoritmo para crear las SpanNearQueries anidadas.
                Ejemplo: sea la query "es un {3} que {2} para"
                1. La representamos como A B g1 C g2 E, donde las letras son los términos y g1 y g2 los huecos.
                   Guardar en una cola el valor de los huecos, en este caso [3,2]
                2. Recorrer los huecos en la expresión 1 posición a la derecha -> A B C g1 E g2 
                   (para tener una especie de notación posfija).
                3. Iniciando de izquierda a derecha, meter a la pila cada elemento de la expresión.
                4. Cuando se llegue a un elemento nulo, hacer una SpanNearQuery
                   con las dos queries que en ese momento estén hasta arriba de la pila,
                   con slop indicado por el siguiente valor de la cola (primero sería 3, luego 2).
                   Eliminar las dos queries que fueron combinadas y dejar la recién creada.
                5. Repetir hasta terminar con todos los elementos de la expresión.
                6. Unir todas las queries que quedan en la pila en una SpanNearQuery con slop 0.
                */ 
                Stack<SpanQuery> pila = new Stack<>();
                
                /*Recorrer huecos*/
                SpanQuery tmp;
                for(int i=0;i<queries.length;i++){
                    if(queries[i] == null){
                        tmp = queries[i+1];
                        queries[i+1] = null;
                        queries[i] = tmp;
                        i++;
                    }
                }
                
                /*Meter elementos a la pila*/
                for(int i=0;i<queries.length;i++){
                    if(queries[i] != null){
                        pila.push(queries[i]);
                    }else{
                        /*Si es hueco construir SpanNearQuery*/
                        SpanQuery b = pila.pop();
                        SpanQuery a = pila.pop();
                        SpanQuery[] clauses = {a,b};
                        SpanNearQuery nestedQuery = new SpanNearQuery(clauses, gaps.poll(), true);
                        pila.push(nestedQuery);
                    }
                }  
                
                /*Query definitiva está formada por las que finalmente quedan en la pila*/
                SpanQuery[] finales = new SpanQuery[pila.size()];
                for(int i=0;i<pila.size();i++){
                    finales[i] = pila.get(i);
                }
                q = new SpanNearQuery(finales, 0, true);
            }
        }
              
        /* Obtener un lector del índice */
        IndexReader reader = indexSearcher.getIndexReader();
        /*TODO: posiblemente la siguiente no es la mejor forma de manejar el reader. 
        Ver http://www.slideshare.net/lucenerevolution/is-your-index-reader-really-atomic-or-maybe-slow */
        AtomicReader wrapper = SlowCompositeReaderWrapper.wrap(reader);
        
        /* Reescribir la query a su forma más rápida. Para el caso de las
        wildcard queries esto es obligatorio o manda excepción más abajo
        (ver http://www.gossamer-threads.com/lists/lucene/java-user/164257)*/
        q = (SpanQuery) q.rewrite(reader);
        
        /* Determinar el conjunto de documentos que cumplen con los filtros solicitados.*/
        Bits bits = null;
        if (!filters.isEmpty()) {
            /* Construir una booleanQuery que represente la estructura de los filtros.
            Los filtros son una lista cuyos elementos son listas de expresiones (campo, operador, valor)
            Esas expresiones son unidas por AND, y las nuevas expresiones resultantes son unidas al final con OR
            */
            //Query que unirá los elementos de la lista de filtros
            BooleanQuery orQuery = new BooleanQuery(); 
            for (ListOfAndFilters list : filters) {
                //Query que unirá las expresiones de cada filtro
                BooleanQuery andQuery = new BooleanQuery();
                for (FilterExpression expression : list) {
                    TermQuery condition = new TermQuery(new Term(expression.getCampo(), expression.getValor()));
                    //Unir por AND (MUST)
                    andQuery.add(condition, BooleanClause.Occur.MUST);
                }
                //Unir por OR (SHOULD)
                orQuery.add(andQuery, BooleanClause.Occur.SHOULD);
            }
            /* Hacer la query y expresar como un conjunto de bits
            el conjunto de documentos resultante. */
            QueryWrapperFilter filterQuery = new QueryWrapperFilter(orQuery);
            DocIdSet docIdset = filterQuery.getDocIdSet(wrapper.getContext(), null);
            DocIdSetIterator itr = docIdset.iterator();
            if (itr != null) {
                OpenBitSet tmpBits = new OpenBitSet();
                while (itr.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    tmpBits.set(itr.docID()); //"prender" el bit correspondiente al documento
                }
                bits = tmpBits;
            }
        } else {
            /* Si no hay filtros prender todos los bits */
            bits = new Bits.MatchAllBits(reader.numDocs());
        }

        /*Obtener spans que se interarán para obtener las concordancias*/ 
        Map<Term, TermContext> termContexts = new HashMap<>();
        Spans spans = q.getSpans(wrapper.getContext(), bits, termContexts);
        
        /*Aquí se contabilizarán las estadísticas de resultados por campo
        para poder generar gráficas de en qué tipo de documentos aparece
        más la petición*/
        this.estadisticasPorCampo = new HashMap<>();
        for(String field : metadataFields){
            estadisticasPorCampo.put(field, new HashMap<>());
        }
        
        List<Concordancia> results = new ArrayList<>();
        int nhits = 0;
        long startTime = System.nanoTime();
        System.out.println("inicio " + startTime);
        
        SortedMap<Integer, NavigableMap<Integer, SpanInfo>> spanMapByDoc = new TreeMap<>();
        SpanInfo spanInfo;
        SpanInfo firstSpan = null;
        boolean first = true;
        while (spans.next() == true) {
            spanInfo = new SpanInfo(spans.start()-window, spans.end()+window, spans.start(), spans.end(), spans.doc());
            if(first){
                firstSpan = spanInfo;
                first=false;
            }
            if(!spanMapByDoc.containsKey(spans.doc())){
                spanMapByDoc.put(spans.doc(), new TreeMap<>());
            }
            spanMapByDoc.get(spans.doc()).put(spans.start()-window, spanInfo);
        }

        for(Integer doc : spanMapByDoc.keySet()){
            for(String field : new String[]{"content", "content_tags", "content_lemmas"}){
                Terms content = reader.getTermVector(doc, field);
                TermsEnum termsEnum = content.iterator(null);

                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    String s = term.utf8ToString();
                    DocsAndPositionsEnum positionsEnum = termsEnum.docsAndPositions(null, null);
                    if (positionsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        int i = 0;
                        int position = -1;
                        while (i < positionsEnum.freq() && (position = positionsEnum.nextPosition()) != -1) {
                            NavigableMap<Integer, SpanInfo> docMap = spanMapByDoc.get(doc);
                            Entry<Integer, SpanInfo> entry = docMap.floorEntry(position);
                            /*Checamos también la key inmediatamente anterior, por si hay traslape.
                                Checamos solo una aunque en teoría podrían existir más
                                traslapes, es decir, la posición podría pertenecer no solo a dos
                                sino a más concordancias, pero sería poco usual. */
                            Entry<Integer, SpanInfo> entryBefore = null;
                            Integer keyBefore = docMap.floorKey(position);
                            if (keyBefore != null){
                                entryBefore = docMap.lowerEntry(docMap.floorKey(position));
                            }
                            for(Entry<Integer, SpanInfo> _entry : new Entry[]{entry,entryBefore}){
                                if(_entry != null){
                                    SpanInfo info = _entry.getValue();
                                    if(position <= info.getEnd()){
                                        info.setEntriesForConcordance(position, s, field);
                                    }
                                }
                            }
                            i++;
                        }
                    }
                }
            }       
        }
       
        for(int doc : spanMapByDoc.keySet()){
            for(Entry<Integer, SpanInfo> entry: spanMapByDoc.get(doc).entrySet()){
                //System.out.println(entry.getValue());
                results.add(entry.getValue().buildConcordance(reader, metadataFields, estadisticasPorCampo, sortFields));
            }
        }
        reader.close();
        long endTime = System.nanoTime();
        System.out.println("fin  " + endTime);
        double duration = (endTime - startTime) / 1000000 ;
        System.out.println("duracion " + duration + " ms");
        Concordancia[] resultsArray = results.toArray(new Concordancia[]{});
        
        /* Se hace un ordenamiento si se especificó al menos un campo */
        if(!(sortFields.length == 1 && "".equals(sortFields[0]))){
            Arrays.sort(resultsArray);
        }
        return resultsArray;
    }

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    public Map<String, Map<String, Integer>> getEstadisticasPorCampo() {
        return estadisticasPorCampo;
    }
    
    

}
