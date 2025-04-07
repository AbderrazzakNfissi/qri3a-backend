package my.project.qri3a.services.impl;

import my.project.qri3a.services.TranslationService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TranslationServiceImpl implements TranslationService {

    // Dictionnaire pour les traductions
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    // Initialisation du dictionnaire avec quelques traductions
    public TranslationServiceImpl() {
        // Initialiser le dictionnaire français
        Map<String, String> frTranslations = new HashMap<>();
        frTranslations.put("notification.new_product", "Une nouvelle annonce correspondant à vos critères a été publiée: %s correspond à votre recherche (%s");
        frTranslations.put("notification.price_range", ", prix entre %d MAD et %d MAD");
        // Ajouter d'autres traductions en français

        // Initialiser le dictionnaire anglais
        Map<String, String> enTranslations = new HashMap<>();
        enTranslations.put("notification.new_product", "A new listing matching your criteria has been published: %s matches your search (%s");
        enTranslations.put("notification.price_range", ", price between %d MAD and %d MAD");
        // Ajouter d'autres traductions en anglais

        // Initialiser le dictionnaire arabe marocain
        Map<String, String> armTranslations = new HashMap<>();
        armTranslations.put("notification.new_product", "إعلان جديد يتوافق مع معاييرك تم نشره: %s يتطابق مع بحثك (%s");
        armTranslations.put("notification.price_range", "، السعر بين %d درهم و %d درهم");
        // Ajouter d'autres traductions en arabe marocain

        // Ajouter les dictionnaires au dictionnaire principal
        translations.put("fr", frTranslations);
        translations.put("en", enTranslations);
        translations.put("arm", armTranslations);
    }

    @Override
    public String translate(String key, String lang) {
        Map<String, String> langTranslations = translations.getOrDefault(lang, translations.get("fr"));
        return langTranslations.getOrDefault(key, key);
    }

    @Override
    public String translate(String key, String lang, Map<String, Object> params) {
        String translation = translate(key, lang);

        // Remplacer les variables dans la traduction
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(translation);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = params.getOrDefault(paramName, matcher.group(0));
            matcher.appendReplacement(result, paramValue.toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }
}