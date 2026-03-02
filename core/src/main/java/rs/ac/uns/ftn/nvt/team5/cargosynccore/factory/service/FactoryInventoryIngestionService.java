package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryInventory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryInventoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryInventoryIngestionService {

    private final FactoryRepository factoryRepository;
    private final FactoryInventoryRepository factoryInventoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void applyProducedAmount(UUID factoryId, UUID productId, long amount, Instant eventTime) {
        if (amount <= 0) {
            return;
        }

        var factory = factoryRepository.findByIdForUpdate(factoryId)
                .orElseThrow(() -> new EntityNotFoundException("Factory not found: " + factoryId));

        if (!productRepository.existsByIdAndFactories_Id(productId, factoryId)) {
            log.warn("Skipping production update for non-associated factory/product pair: {}/{}", factoryId, productId);
            return;
        }

        LocalDateTime eventDateTime = LocalDateTime.ofInstant(eventTime, ZoneOffset.UTC);

        FactoryInventory inventory = factoryInventoryRepository
                .findByFactoryIdAndProductIdForUpdate(factoryId, productId)
                .orElseGet(() -> {
                    FactoryInventory created = new FactoryInventory();
                    created.setFactory(factory);
                    created.setProduct(productRepository.getReferenceById(productId));
                    created.setTotalProduced(0L);
                    return created;
                });

        long updatedTotal = (inventory.getTotalProduced() == null ? 0L : inventory.getTotalProduced()) + amount;
        inventory.setTotalProduced(updatedTotal);

        LocalDateTime previous = inventory.getLastProductionTime();
        if (previous == null || eventDateTime.isAfter(previous)) {
            inventory.setLastProductionTime(eventDateTime);
        }

        factoryInventoryRepository.save(inventory);
    }
}
