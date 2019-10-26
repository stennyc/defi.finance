package finance.defi.service.impl;

import com.google.gson.Gson;
import finance.defi.domain.Asset;
import finance.defi.domain.User;
import finance.defi.domain.enumeration.TransactionType;
import finance.defi.repository.AssetRepository;
import finance.defi.service.TransactionService;
import finance.defi.domain.Transaction;
import finance.defi.repository.TransactionRepository;
import finance.defi.service.UserService;
import finance.defi.service.dto.RawTransactionDTO;
import finance.defi.service.dto.TransactionDTO;
import finance.defi.service.dto.TransactionHashDTO;
import finance.defi.service.mapper.TransactionMapper;
import finance.defi.service.util.ConverterUtil;
import finance.defi.service.util.TransactionDecoderUtil;
import finance.defi.web.rest.errors.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.parity.Parity;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import javax.persistence.metamodel.SingularAttribute;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static finance.defi.domain.Transaction_.amount;
import static finance.defi.domain.Transaction_.asset;

/**
 * Service Implementation for managing {@link Transaction}.
 */
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Value("${ether.network.provider}")
    private String networkProvider;

    private static Web3jService service;

    private static Web3j web3j;

    private static Parity parity;

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;

    private final UserService userService;

    private final AssetRepository assetRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TransactionMapper transactionMapper,
                                  UserService userService,
                                  AssetRepository assetRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.userService = userService;
        this.assetRepository = assetRepository;

        this.service = new HttpService("https://rinkeby.infura.io/v3/4750a8f14c37429687f5229ff94e4e56");
        this.web3j = Web3j.build(this.service);
        this.parity = Parity.build(this.service);
    }

    /**
     * Save a transaction.
     *
     * @param transaction the entity to save.
     * @return the persisted entity.
     */
    public TransactionDTO save(Transaction transaction) {
        log.debug("Request to save Transaction : {}", transaction);
        transaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(transaction);
    }

    /**
     * Get all the transactions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Transactions");
        return transactionRepository.findAll(pageable)
            .map(transactionMapper::toDto);
    }


    /**
     * Get one transaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionDTO> findOne(Long id) {
        log.debug("Request to get Transaction : {}", id);
        return transactionRepository.findById(id)
            .map(transactionMapper::toDto);
    }

    @Override
    public TransactionHashDTO processRawTransaction(RawTransactionDTO rawTransactionDTO) {

        Gson gson = new Gson();
        EthSendTransaction ethSendTx = null;
        RawTransaction encodedRawTx = TransactionDecoderUtil.decode(rawTransactionDTO.getTx());
        BigDecimal amount = new BigDecimal(ConverterUtil.fromWei(encodedRawTx.getValue(), Convert.Unit.ETHER));

        //TODO validate 2FA
        //TODO validate balance
        //TODO validate daily transfer limit
        //TODO validate trusted device

        User currentUser = userService.getUserWithAuthorities().orElseThrow(
            () -> new EntityNotFoundException("User not found"));

        Asset asset = assetRepository.findByNameAndIsVisible(rawTransactionDTO.getAsset(), true).orElseThrow(
            () -> new EntityNotFoundException("Asset not found"));

        // send the signed transaction to the ethereum client
        try {
            log.info(rawTransactionDTO.getTx());
            ethSendTx = web3j
                .ethSendRawTransaction(rawTransactionDTO.getTx())
                .sendAsync()
                .get();

            if (null != ethSendTx) {
                log.info("saving transaction to db");

                // save transaction
                saveTransaction(amount,
                    asset,
                    currentUser,
                    TransactionType.SUPPLY,
                    ethSendTx.getTransactionHash(),
                    gson.toJson(encodedRawTx));
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new TransactionHashDTO(ethSendTx.getTransactionHash());
    }

    private void saveTransaction(BigDecimal amount,
                                 Asset asset,
                                 User currentUser,
                                 TransactionType type,
                                 String transactionHash,
                                 String encodedTx) {

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setAsset(asset);
        transaction.setCreatedAt(Instant.now());
        transaction.setTransactionType(type);
        transaction.setUser(currentUser);
        transaction.setTxHash(transactionHash);
        transaction.setTxRaw(encodedTx);

        save(transaction);
    }
}