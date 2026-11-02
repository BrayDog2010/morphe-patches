package app.braydog2010.extension.spotify.layout.hide.createbutton;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceType;
import app.braydog2010.extension.spotify.shared.ComponentFilters.ComponentFilter;
import app.braydog2010.extension.spotify.shared.ComponentFilters.ResourceIdComponentFilter;
import app.braydog2010.extension.spotify.shared.ComponentFilters.StringComponentFilter;

import java.util.List;

@Deprecated(forRemoval = true)
@SuppressWarnings("unused")
public final class HideCreateButtonPatch {

    private static final List<ComponentFilter> CREATE_BUTTON_COMPONENT_FILTERS = List.of(
            new ResourceIdComponentFilter(ResourceType.STRING, "navigationbar_musicappitems_create_title"),
            new StringComponentFilter("spotify:create-menu")
    );

    private static final ResourceIdComponentFilter OLD_CREATE_BUTTON_COMPONENT_FILTER =
            new ResourceIdComponentFilter(ResourceType.STRING, "bottom_navigation_bar_create_tab_title");

    public static Object returnNullIfIsCreateButton(Object navigationBarItem) {
        if (navigationBarItem == null) {
            return null;
        }

        try {
            String stringifiedNavigationBarItem = navigationBarItem.toString();

            for (ComponentFilter componentFilter : CREATE_BUTTON_COMPONENT_FILTERS) {
                if (componentFilter.filterUnavailable()) {
                    Logger.printInfo(() -> "returnNullIfIsCreateButton: Filter " +
                            componentFilter.getFilterRepresentation() + " not available, skipping");
                    continue;
                }

                if (stringifiedNavigationBarItem.contains(componentFilter.getFilterValue())) {
                    Logger.printInfo(() -> "Hiding Create button because the navigation bar item " +
                            navigationBarItem + " matched the filter " + componentFilter.getFilterRepresentation());
                    return null;
                }
            }
        } catch (Throwable ex) {
            Logger.printException(() -> "returnNullIfIsCreateButton failure", ex);
        }

        return navigationBarItem;
    }

    public static boolean isOldCreateButton(int oldNavigationBarItemTitleResId) {
        if (OLD_CREATE_BUTTON_COMPONENT_FILTER.filterUnavailable()) {
            Logger.printInfo(() -> "Skipping hiding old Create button because the resource id for " +
                    OLD_CREATE_BUTTON_COMPONENT_FILTER.resourceName + " is not available");
            return false;
        }

        if (oldNavigationBarItemTitleResId == OLD_CREATE_BUTTON_COMPONENT_FILTER.getResourceId()) {
            Logger.printInfo(() -> "Hiding old Create button because the navigation bar item title resource id" +
                    " matched " + OLD_CREATE_BUTTON_COMPONENT_FILTER.getFilterRepresentation());
            return true;
        }

        return false;
    }
}
