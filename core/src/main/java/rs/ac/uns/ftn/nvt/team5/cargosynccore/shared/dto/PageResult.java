package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Redis-serializable alternative to Spring's {@link Page}.
 * {@code PageImpl} cannot be deserialized by Jackson (no usable constructor),
 * so we wrap paginated results in this plain DTO before caching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> content;
    private long totalElements;
    private int pageNumber;
    private int pageSize;

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }

    public Page<T> toPage(Pageable pageable) {
        return new PageImpl<>(content, pageable, totalElements);
    }
}
