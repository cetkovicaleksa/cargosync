package rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto.AccountDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.CreateManagerRequest;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.dto.TemporaryPasswordDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.service.ManagerService;

@RestController
@RequestMapping("/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_MANAGERS')")
    public TemporaryPasswordDto createManager(
            @RequestPart("data") @Valid CreateManagerRequest request,
            @RequestPart("avatar") MultipartFile avatar
    ) {
        String tempPass = managerService.createManager(request, avatar);
        return new TemporaryPasswordDto(tempPass);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_MANAGERS')")
    public Page<AccountDto> getAllManagers(@RequestParam(required = false) String query, Pageable pageable) {
        return managerService.searchManagers(query, pageable)
                .map(account -> modelMapper.map(account, AccountDto.class));
    }

    @PutMapping("/{username}/toggle-block")
    @PreAuthorize("hasAuthority('MANAGE_MANAGERS')")
    public void blockManager(@PathVariable String username) {
        managerService.toggleBlockManager(username);
    }
}