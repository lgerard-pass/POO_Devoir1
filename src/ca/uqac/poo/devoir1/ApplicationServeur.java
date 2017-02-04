package ca.uqac.poo.devoir1;
import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by dhawo on 03/02/2017.
 */
public class ApplicationServeur {
    private ServerSocket welcomeSocket = null;
    private String sourceFolder;
    private String classFolder;
    private String outputFile;
    private Socket connectionSocket;
    private HashMap<String,Object> objects;

    /**
     * prend le numéro de port, crée un SocketServer sur le port
     */
    public ApplicationServeur (int port){
        try{
            welcomeSocket = new ServerSocket(port);
            objects = new HashMap<>();
        }catch(IOException ex){
            log("Le Socket n'as pas pu être bindé sur le port donné.");
        }
    }

    /**
     * Se met en attente de connexions des clients. Suite aux connexions, elle lit
     * ce qui est envoyé à travers la Socket, recrée l’objet Commande envoyé par
     * le client, et appellera traiterCommande(Commande uneCommande)
     */
    public void aVosOrdres() {
        while(true){
            try{
                connectionSocket = welcomeSocket.accept(); //Accepte la connexion (une seule à la fois dans ce cas)
                ObjectInputStream inputFromClient = new ObjectInputStream(connectionSocket.getInputStream()); //Créer le Stream d'entrée
                Commande commande = (Commande)inputFromClient.readObject(); //Récupération de la commande
                traiteCommande(commande); //Traitement de la commande
                connectionSocket.close();
            }catch(IOException ex){
                System.out.println("La connexion n'as pas pu etre acceptée");
            }catch(ClassNotFoundException ex){
                System.out.println("Le message recu n'est pas une commande");
            }
        }
    }

    /**
     * prend uneCommande dument formattée, et la traite. Dépendant du type de commande,
     * elle appelle la méthode spécialisée
     */
    public void traiteCommande(Commande uneCommande) {
        switch(uneCommande.getType()){
            case "lecture":
                String id = uneCommande.getArgument(0);
                String attribut = uneCommande.getArgument(1);
                Object pointeurObject = objects.get(id);
                traiterLecture(pointeurObject,attribut);
                break;
            case "ecriture":
                break;
            case "creation":
                break;
            case "chargement":
                String nomQualifie = uneCommande.getArgument(0);
                traiterChargement(nomQualifie);
                break;
            case "compilation":
                String[] sourcesFiles = uneCommande.getArgument(0).split(",");
                String classPath = uneCommande.getArgument(1);
                for(String sourceFile : sourcesFiles){
                    traiterCompilation(sourceFile);
                }
                break;
            case "appel":
                break;
        }
    }

    /**
     * traiterLecture : traite la lecture d’un attribut. Renvoies le résultat par le
     * socket
     */
    public void traiterLecture(Object pointeurObjet, String attribut) {
        Class c = pointeurObjet.getClass();
        try{
            Field field = c.getField(attribut);
            Object value = (field.getType().cast(field.get(pointeurObjet)));
            ObjectOutputStream outputToClient = new ObjectOutputStream(connectionSocket.getOutputStream()); //Création du Stream de sortie
            outputToClient.writeObject(value);
        }catch(NoSuchFieldException ex){
            log(ex.getMessage());
        }catch(IllegalAccessException ex){
            log(ex.getMessage());
        }catch(IOException ex){
            log(ex.getMessage());
        }
    }

    /**
     * traiterEcriture : traite l’écriture d’un attribut. Confirmes au client que l’écriture
     * s’est faite correctement.
     */
    public void traiterEcriture(Object pointeurObjet, String attribut, Object valeur) {

    }

    /**
     * traiterCreation : traite la création d’un objet. Confirme au client que la création
     * s’est faite correctement.
     */
    public void traiterCreation(Class classeDeLobjet, String identificateur) {

    }

    /**
     * traiterChargement : traite le chargement d’une classe. Confirmes au client que la création
     * s’est faite correctement.
     */
    public void traiterChargement(String nomQualifie) {
        try{
            File file = new File(classFolder);
            // Convert File to a URL
            URL url = file.toURI().toURL(); //
            URL[] urls = new URL[] { url };
            ClassLoader loader = new URLClassLoader(urls);
            Class thisClass = loader.loadClass(nomQualifie);
            ObjectOutputStream outputToClient = new ObjectOutputStream(connectionSocket.getOutputStream()); //Création du Stream de sortie
            outputToClient.writeObject(new Boolean(true));
        }catch (IOException ex){
            log(ex.getMessage());
        }catch (ClassNotFoundException ex){

        }
    }

    /**
     * traiterCompilation : traite la compilation d’un fichier source java. Confirme au client
     * que la compilation s’est faite correctement. Le fichier source est donné par son chemin
     * relatif par rapport au chemin des fichiers sources.
     */
    public void traiterCompilation(String cheminRelatifFichierSource) {
        try{
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            File file = new File(sourceFolder + File.pathSeparator + cheminRelatifFichierSource);
            StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null);

            String[] options = new String[] { "-d", classFolder };
            File[] javaFiles = new File[] { file };

            JavaCompiler.CompilationTask compilationTask = compiler.getTask(null, null, null,
                    Arrays.asList(options),
                    null,
                    sjfm.getJavaFileObjects(javaFiles)
            );

            compilationTask.call();

            ObjectOutputStream outputToClient = new ObjectOutputStream(connectionSocket.getOutputStream()); //Création du Stream de sortie
            outputToClient.writeObject(new Boolean(true));
        }catch (IOException ex){
            log(ex.getMessage());
        }

    }

    /**
     * traiterAppel : traite l’appel d’une méthode, en prenant comme argument l’objet
     * sur lequel on effectue l’appel, le nom de la fonction à appeler, un tableau de nom de
     * types des arguments, et un tableau d’arguments pour la fonction. Le résultat de la
     * fonction est renvoyé par le serveur au client (ou le message que tout s’est bien
     * passé)
     */
    public void traiterAppel(Object pointeurObjet, String nomFonction, String[] types, Object[] valeurs){

    }

    /**
     * programme principal. Prend 4 arguments: 1) numéro de port, 2) répertoire source, 3)
     * répertoire classes, et 4) nom du fichier de traces (sortie)
     * Cette méthode doit créer une instance de la classe ApplicationServeur, l’initialiser
     * puis appeler aVosOrdres sur cet objet
     */
    public static void main(String[] args) {
        if(args.length != 4){
            System.out.println("Mauvais arguments");
            System.out.println("Utilisation : ");
            System.out.println("1) numéro de port, 2) répertoire source, 3) répertoire classes, et 4) nom du fichier de traces (sortie)");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ApplicationServeur serveur = new ApplicationServeur(port);
        serveur.sourceFolder = args[1];
        serveur.classFolder = args[2];
        serveur.outputFile = args[3];
        if(serveur.welcomeSocket != null){
            serveur.aVosOrdres();
        }
    }

    private void log(String message){
        try
        {
            FileWriter fw = new FileWriter(outputFile);
            fw.write (LocalDateTime.now().toString() + message);
            fw.write ("\r\n");
            fw.close();
        }
        catch (IOException exception)
        {
            System.out.println ("Erreur lors de l'écriture : " + exception.getMessage());
        }
    }
}