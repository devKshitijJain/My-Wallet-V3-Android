package piuk.blockchain.androidbuysell.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExchangeData {

    public ExchangeData() {
        // Empty constructor
    }

    @JsonProperty("coinify")
    private CoinifyData coinify = null;

    @Nullable
    public CoinifyData getCoinify() {
        return coinify;
    }

    public void setCoinify(CoinifyData coinify) {
        this.coinify = coinify;
    }
}
