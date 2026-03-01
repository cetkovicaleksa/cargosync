package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import org.springframework.stereotype.Service;

@Service
public class HttpUtil {

    public int parseEtag(String header) {
        if (header.startsWith("W/")) {
            header = header.substring(2);
        }

        header = header.replace("\"", "");

        return Integer.parseUnsignedInt(header, 16);
    }

    public String serializeEtag(int version) {
        return Integer.toHexString(version);
    }

}
