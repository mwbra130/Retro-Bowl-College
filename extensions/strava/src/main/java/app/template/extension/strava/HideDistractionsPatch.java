/*
 * Ported from the ReVanced/De-Vanced Strava "Hide distractions" extension
 * (GPLv3). Adapted to this repository's extension package and verified
 * against the Strava 486.9 modular framework classes.
 */

package app.template.extension.strava;

import android.annotation.SuppressLint;

import com.strava.modularframework.data.Destination;
import com.strava.modularframework.data.GenericLayoutModule;
import com.strava.modularframework.data.GenericModuleField;
import com.strava.modularframework.data.ListField;
import com.strava.modularframework.data.ListProperties;
import com.strava.modularframework.data.ModularComponent;
import com.strava.modularframework.data.ModularEntry;
import com.strava.modularframework.data.ModularEntryContainer;
import com.strava.modularframework.data.ModularMenuItem;
import com.strava.modularframework.data.Module;
import com.strava.modularframework.data.MultiStateFieldDescriptor;
import com.strava.modularframeworknetwork.ModularEntryNetworkContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressLint("NewApi")
public class HideDistractionsPatch {
    public static boolean upselling;
    public static boolean promo;
    public static boolean followSuggestions;
    public static boolean challengeSuggestions;
    public static boolean joinChallenge;
    public static boolean joinClub;
    public static boolean activityLookback;

    // The modular model objects are deserialized from server JSON, so any of
    // the intercepted getters can return null at runtime even where the Kotlin
    // type is non-null. Every filter below passes a null result through
    // unchanged instead of dereferencing it: the wrapper must behave exactly
    // like the original getter when nothing is being hidden.

    public static List<ModularEntry> filterChildrenEntries(ModularEntry modularEntry) {
        if (hideModularEntry(modularEntry)) {
            return Collections.emptyList();
        }
        List<ModularEntry> childrenEntries = modularEntry.getChildrenEntries$original();
        if (childrenEntries == null) {
            return null;
        }
        return childrenEntries.stream()
                .filter(childrenEntry -> !hideModularEntry(childrenEntry))
                .collect(Collectors.toList());
    }

    public static List<ModularEntry> filterEntries(ModularEntryContainer modularEntryContainer) {
        if (hideModularEntryContainer(modularEntryContainer)) {
            return Collections.emptyList();
        }
        List<ModularEntry> entries = modularEntryContainer.getEntries$original();
        if (entries == null) {
            return null;
        }
        return entries.stream()
                .filter(entry -> !hideModularEntry(entry))
                .collect(Collectors.toList());
    }

    public static List<ModularEntry> filterEntries(ModularEntryNetworkContainer modularEntryNetworkContainer) {
        if (hideModularEntryNetworkContainer(modularEntryNetworkContainer)) {
            return Collections.emptyList();
        }
        List<ModularEntry> entries = modularEntryNetworkContainer.getEntries$original();
        if (entries == null) {
            return null;
        }
        return entries.stream()
                .filter(entry -> !hideModularEntry(entry))
                .collect(Collectors.toList());
    }

    public static List<ModularMenuItem> filterMenuItems(ModularEntryContainer modularEntryContainer) {
        if (hideModularEntryContainer(modularEntryContainer)) {
            return Collections.emptyList();
        }
        List<ModularMenuItem> menuItems = modularEntryContainer.getMenuItems$original();
        if (menuItems == null) {
            return null;
        }
        return menuItems.stream()
                .filter(menuItem -> !hideModularMenuItem(menuItem))
                .collect(Collectors.toList());
    }

    public static ListProperties filterProperties(ModularEntryContainer modularEntryContainer) {
        if (hideModularEntryContainer(modularEntryContainer)) {
            return null;
        }
        return modularEntryContainer.getProperties$original();
    }

    public static ListProperties filterProperties(ModularEntryNetworkContainer modularEntryNetworkContainer) {
        if (hideModularEntryNetworkContainer(modularEntryNetworkContainer)) {
            return null;
        }
        return modularEntryNetworkContainer.getProperties$original();
    }

    public static ListField filterField(ListProperties listProperties, String key) {
        ListField listField = listProperties.getField$original(key);
        if (hideListField(listField)) {
            return null;
        }
        return listField;
    }

    public static List<ListField> filterFields(ListField listField) {
        if (hideListField(listField)) {
            return null;
        }
        List<ListField> fields = listField.getFields$original();
        if (fields == null) {
            return null;
        }
        return fields.stream()
                .filter(field -> !hideListField(field))
                .collect(Collectors.toList());
    }

    public static List<Module> filterModules(ModularEntry modularEntry) {
        if (hideModularEntry(modularEntry)) {
            return Collections.emptyList();
        }
        List<Module> modules = modularEntry.getModules$original();
        if (modules == null) {
            return null;
        }
        return modules.stream()
                .filter(module -> !hideModule(module))
                .collect(Collectors.toList());
    }

    public static GenericModuleField filterField(GenericLayoutModule genericLayoutModule, String key) {
        if (hideGenericLayoutModule(genericLayoutModule)) {
            return null;
        }
        GenericModuleField field = genericLayoutModule.getField$original(key);
        if (hideGenericModuleField(field)) {
            return null;
        }
        return field;
    }

    public static GenericModuleField[] filterFields(GenericLayoutModule genericLayoutModule) {
        if (hideGenericLayoutModule(genericLayoutModule)) {
            return new GenericModuleField[0];
        }
        GenericModuleField[] fields = genericLayoutModule.getFields$original();
        if (fields == null) {
            return null;
        }
        return Arrays.stream(fields)
                .filter(field -> !hideGenericModuleField(field))
                .toArray(GenericModuleField[]::new);
    }

    public static GenericLayoutModule[] filterSubmodules(GenericLayoutModule genericLayoutModule) {
        if (hideGenericLayoutModule(genericLayoutModule)) {
            return new GenericLayoutModule[0];
        }
        GenericLayoutModule[] submodules = genericLayoutModule.getSubmodules$original();
        if (submodules == null) {
            return null;
        }
        return Arrays.stream(submodules)
                .filter(submodule -> !hideGenericLayoutModule(submodule))
                .toArray(GenericLayoutModule[]::new);
    }

    public static List<Module> filterSubmodules(ModularComponent modularComponent) {
        if (hideByName(modularComponent.getPage()) || hideByName(modularComponent.getElement())) {
            return Collections.emptyList();
        }
        List<Module> submodules = modularComponent.getSubmodules$original();
        if (submodules == null) {
            return null;
        }
        return submodules.stream()
                .filter(submodule -> !hideModule(submodule))
                .collect(Collectors.toList());
    }

    public static Map<String, GenericModuleField> filterStateMap(MultiStateFieldDescriptor multiStateFieldDescriptor) {
        Map<String, GenericModuleField> stateMap = multiStateFieldDescriptor.getStateMap$original();
        if (stateMap == null) {
            return null;
        }
        return stateMap.entrySet().stream()
                .filter(entry -> !hideGenericModuleField(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean hideModule(Module module) {
        return module == null ||
                hideByName(module.getPage()) ||
                hideByName(module.getElement());
    }

    private static boolean hideModularEntry(ModularEntry modularEntry) {
        return modularEntry == null ||
                hideByName(modularEntry.getPage()) ||
                hideByName(modularEntry.getElement()) ||
                hideByDestination(modularEntry.getDestination());
    }

    private static boolean hideGenericLayoutModule(GenericLayoutModule genericLayoutModule) {
        try {
            return genericLayoutModule == null ||
                    hideByName(genericLayoutModule.getPage()) ||
                    hideByName(genericLayoutModule.getElement()) ||
                    hideByDestination(genericLayoutModule.getDestination());
        } catch (RuntimeException getParentEntryOrThrowException) {
            return false;
        }
    }

    private static boolean hideListField(ListField listField) {
        return listField == null ||
                hideByName(listField.getElement()) ||
                hideByDestination(listField.getDestination());
    }

    private static boolean hideGenericModuleField(GenericModuleField genericModuleField) {
        return genericModuleField == null ||
                hideByName(genericModuleField.getElement()) ||
                hideByDestination(genericModuleField.getDestination());
    }

    private static boolean hideModularEntryContainer(ModularEntryContainer modularEntryContainer) {
        return modularEntryContainer == null ||
                hideByName(modularEntryContainer.getPage());
    }

    private static boolean hideModularEntryNetworkContainer(ModularEntryNetworkContainer modularEntryNetworkContainer) {
        return modularEntryNetworkContainer == null ||
                hideByName(modularEntryNetworkContainer.getPage());
    }

    private static boolean hideModularMenuItem(ModularMenuItem modularMenuItem) {
        return modularMenuItem == null ||
                hideByName(modularMenuItem.getElementName()) ||
                hideByDestination(modularMenuItem.getDestination());
    }

    private static boolean hideByName(String name) {
        return name != null && (
                upselling && name.contains("_upsell") ||
                        promo && (name.equals("promo") || name.equals("top_of_tab_promo")) ||
                        followSuggestions && name.equals("suggested_follows") ||
                        challengeSuggestions && name.equals("suggested_challenges") ||
                        joinChallenge && name.equals("challenge") ||
                        joinClub && name.equals("club") ||
                        activityLookback && name.equals("highlighted_activity_lookback")
        );
    }

    private static boolean hideByDestination(Destination destination) {
        if (destination == null) {
            return false;
        }
        String url = destination.getUrl();
        return url != null && (
                upselling && url.startsWith("strava://subscription/checkout")
        );
    }
}
