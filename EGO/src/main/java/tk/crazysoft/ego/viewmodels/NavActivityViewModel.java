package tk.crazysoft.ego.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;

import tk.crazysoft.ego.BR;

public class NavActivityViewModel extends BaseObservable {
    private boolean isCalculatingRoute;
    private Drawable directionSymbol, nextDirectionSymbol;
    private String direction, street, arrivalTime, arrivalDistance;

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
        notifyChange();
    }

    @Bindable
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
        notifyPropertyChanged(BR.street);
        notifyChange();
    }

    @Bindable
    public Drawable getNextDirectionSymbol() {
        return nextDirectionSymbol;
    }

    public void setNextDirectionSymbol(Drawable nextDirectionSymbol) {
        boolean nullSwitch = nextDirectionSymbol == null && this.nextDirectionSymbol != null || nextDirectionSymbol != null && this.nextDirectionSymbol == null;
        this.nextDirectionSymbol = nextDirectionSymbol;
        notifyPropertyChanged(BR.nextDirectionSymbol);
        if (nullSwitch) {
            notifyPropertyChanged(BR.hasNextDirection);
        }
    }

    @Bindable
    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
        notifyPropertyChanged(BR.arrivalTime);
    }

    @Bindable
    public String getArrivalDistance() {
        return arrivalDistance;
    }

    public void setArrivalDistance(String arrivalDistance) {
        this.arrivalDistance = arrivalDistance;
        notifyPropertyChanged(BR.arrivalDistance);
    }

    @Bindable
    public boolean isHasNextDirection() {
        return !isCalculatingRoute && nextDirectionSymbol != null;
    }
}
