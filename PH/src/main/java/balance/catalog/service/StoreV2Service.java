package balance.catalog.service;

import balance.catalog.dto.StoreRequestDTO;
import balance.catalog.dto.StoreResponseDTO;
import balance.model.Store;
import balance.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StoreV2Service {

    @Autowired
    private StoreRepository storeRepository;

    public List<StoreResponseDTO> findAll() {
        return storeRepository.findAll()
                .stream()
                .map(StoreResponseDTO::from)
                .toList();
    }

    public List<StoreResponseDTO> findAllActive() {
        return storeRepository.findAll()
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .map(StoreResponseDTO::from)
                .toList();
    }

    public Optional<StoreResponseDTO> findById(Long id) {
        return storeRepository.findById(id).map(StoreResponseDTO::from);
    }

    @Transactional
    public StoreResponseDTO create(StoreRequestDTO dto) {
        Store store = new Store();
        store.setName(dto.getName().trim());
        store.setAddress(dto.getAddress());
        store.setPhone(dto.getPhone());
        store.setActive(true);
        return StoreResponseDTO.from(storeRepository.save(store));
    }

    @Transactional
    public Optional<StoreResponseDTO> update(Long id, StoreRequestDTO dto) {
        return storeRepository.findById(id).map(store -> {
            store.setName(dto.getName().trim());
            store.setAddress(dto.getAddress());
            store.setPhone(dto.getPhone());
            return StoreResponseDTO.from(storeRepository.save(store));
        });
    }

    @Transactional
    public Optional<StoreResponseDTO> toggle(Long id) {
        return storeRepository.findById(id).map(store -> {
            store.setActive(!Boolean.TRUE.equals(store.getActive()));
            return StoreResponseDTO.from(storeRepository.save(store));
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!storeRepository.existsById(id)) return false;
        storeRepository.deleteById(id);
        return true;
    }
}
