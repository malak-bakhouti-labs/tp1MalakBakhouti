package ma.formations.malak.tp1malakbakhouti.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.formations.malak.tp1malakbakhouti.llm.JsonAdapterPourGemini;
import ma.formations.malak.tp1malakbakhouti.llm.LlmInteraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable.
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    /**
     * Rôle système envoyé à Gemini.
     * Possible d'écrire un nouveau rôle dans la liste déroulante.
     */
    private String roleSysteme;

    /**
     * Le rôle système peut être choisi au début de chaque conversation.
     */
    private boolean roleSystemeChangeable = true;

    /**
     * Liste de tous les rôles de l'API prédéfinis.
     */
    private List<SelectItem> listeRolesSysteme;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;

    /**
     * Dernière réponse de l'API Gemini.
     */
    private String reponse;

    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Prépare les requêtes JSON et extrait les réponses de Gemini.
     */
    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    /**
     * Contexte JSF utilisé pour afficher les messages d'erreur dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Constructeur sans argument du bean CDI.
     */
    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    /**
     * Setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question à Gemini et met à jour la conversation affichée.
     *
     * @return null pour rester sur la même page
     */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Texte question vide",
                    "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        if (this.conversation.isEmpty()) {
            jsonAdapter.setSystemRole(this.roleSysteme);
            this.roleSystemeChangeable = false;
        }

        try {
            LlmInteraction interaction = jsonAdapter.envoyerRequete(this.question);
            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.questionJson();
            this.texteReponseJson = interaction.reponseJson();
        } catch (Exception e) {
            this.reponse = "Erreur : " + e.getMessage();
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API du LLM",
                    "Problème de connexion avec l'API du LLM" + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            facesContext.addMessage(null, message);
        }

        afficherConversation();
        return null;
    }

    /**
     * Ouvre une nouvelle vue pour commencer une nouvelle conversation.
     *
     * @return la vue index
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Ajoute la question et la réponse à la conversation affichée.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis.
            this.listeRolesSysteme = new ArrayList<>();
            // Vous pouvez évidemment écrire ces rôles dans la langue que vous voulez.
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));
        }

        return this.listeRolesSysteme;
    }
}