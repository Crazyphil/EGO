package tk.crazysoft.ego.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;

import tk.crazysoft.ego.BR;

public class NavActivityViewModel extends BaseObservable {
    private boolean isCalculatingRoute;
    private Drawable directionSymbol;
    private String direction, street, nextDirection;

    @Bindable
    public boolean isCalculatingRoute() {
        return isCalculatingRoute;
    }

    @Bindable
    public void setCalculatingRoute(boolean calculatingRoute) {
        isCalculatingRoute = calculatingRoute;
        notifyPropertyChanged(BR.calculatingRoute);
    }


    @Bindable
    public Drawable getDirectionSymbol() {
        return directionSymbol;
    }

    public void setDirectionSymbol(Drawable directionSymbol) {
        this.directionSymbol = directionSymbol;
        notifyPropertyChanged(BR.directionSymbol);
    }

    @Bindable
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
        notifyPropertyChanged(BR.direction);
    }

    @Bindable
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
        notifyPropertyChanged(BR.street);
    }

    @Bindable
    public String getNextDirection() {
        return nextDirection;
    }

    public void setNextDirection(String nextDirection) {
        this.nextDirection = nextDirection;
        notifyPropertyChanged(BR.nextDirection);
        notifyPropertyChanged(BR.hasNextDirection);
    }

    @Bindable
    public boolean isHasNextDirection() {
        return nextDirection != null;
    }
}
