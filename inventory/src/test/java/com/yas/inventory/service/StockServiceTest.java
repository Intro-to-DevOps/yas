package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    @Test
    void addProductIntoWarehouse_whenValidInput_shouldSaveStockWithDefaultQuantities() {
        StockPostVm postVm = new StockPostVm(10L, 20L);
        Warehouse warehouse = Warehouse.builder().id(20L).name("Main Warehouse").build();

        when(stockRepository.existsByWarehouseIdAndProductId(20L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Keyboard", "KB-01", false));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(postVm));

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        Stock savedStock = captor.getValue().getFirst();
        assertThat(savedStock.getProductId()).isEqualTo(10L);
        assertThat(savedStock.getWarehouse().getId()).isEqualTo(20L);
        assertThat(savedStock.getQuantity()).isEqualTo(0L);
        assertThat(savedStock.getReservedQuantity()).isEqualTo(0L);
    }

    @Test
    void addProductIntoWarehouse_whenStockAlreadyExists_shouldThrowStockExistingException() {
        StockPostVm postVm = new StockPostVm(10L, 20L);
        when(stockRepository.existsByWarehouseIdAndProductId(20L, 10L)).thenReturn(true);

        assertThrows(StockExistingException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));
    }

    @Test
    void addProductIntoWarehouse_whenProductNotFound_shouldThrowNotFoundException() {
        StockPostVm postVm = new StockPostVm(10L, 20L);

        when(stockRepository.existsByWarehouseIdAndProductId(20L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseNotFound_shouldThrowNotFoundException() {
        StockPostVm postVm = new StockPostVm(10L, 20L);

        when(stockRepository.existsByWarehouseIdAndProductId(20L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Keyboard", "KB-01", false));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_shouldReturnMappedStockViewModels() {
        Long warehouseId = 20L;
        ProductInfoVm product = new ProductInfoVm(10L, "Keyboard", "KB-01", true);
        Warehouse warehouse = Warehouse.builder().id(warehouseId).name("Main Warehouse").build();
        Stock stock = Stock.builder()
            .id(100L)
            .productId(10L)
            .quantity(25L)
            .reservedQuantity(5L)
            .warehouse(warehouse)
            .build();

        when(warehouseService.getProductWarehouse(warehouseId, "Key", "KB", FilterExistInWhSelection.YES))
            .thenReturn(List.of(product));
        when(stockRepository.findByWarehouseIdAndProductIdIn(warehouseId, List.of(10L))).thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(warehouseId, "Key", "KB");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().productName()).isEqualTo("Keyboard");
        assertThat(result.getFirst().warehouseId()).isEqualTo(warehouseId);
    }

    @Test
    void updateProductQuantityInStock_whenValidRequest_shouldUpdateStockAndPublishChanges() {
        Warehouse warehouse = Warehouse.builder().id(20L).name("Main Warehouse").build();
        Stock stock1 = Stock.builder().id(1L).productId(10L).quantity(10L).reservedQuantity(0L).warehouse(warehouse).build();
        Stock stock2 = Stock.builder().id(2L).productId(20L).quantity(5L).reservedQuantity(0L).warehouse(warehouse).build();
        List<StockQuantityVm> updates = List.of(
            new StockQuantityVm(1L, 3L, "received"),
            new StockQuantityVm(2L, null, "no-change")
        );

        when(stockRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(stock1, stock2));
        when(stockRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        stockService.updateProductQuantityInStock(new StockQuantityUpdateVm(updates));

        assertThat(stock1.getQuantity()).isEqualTo(13L);
        assertThat(stock2.getQuantity()).isEqualTo(5L);
        verify(stockHistoryService).createStockHistories(List.of(stock1, stock2), updates);

        ArgumentCaptor<List<ProductQuantityPostVm>> productQuantityCaptor = ArgumentCaptor.forClass(List.class);
        verify(productService).updateProductQuantity(productQuantityCaptor.capture());
        assertThat(productQuantityCaptor.getValue()).hasSize(2);
        assertThat(productQuantityCaptor.getValue().getFirst().stockQuantity()).isEqualTo(13L);
    }

    @Test
    void updateProductQuantityInStock_whenNoStockFound_shouldNotCallProductUpdate() {
        List<StockQuantityVm> updates = List.of(new StockQuantityVm(999L, 1L, "adjust"));
        when(stockRepository.findAllById(List.of(999L))).thenReturn(List.of());

        stockService.updateProductQuantityInStock(new StockQuantityUpdateVm(updates));

        verify(stockRepository).saveAll(List.of());
        verify(stockHistoryService).createStockHistories(List.of(), updates);
        verify(productService, never()).updateProductQuantity(any());
    }

    @Test
    void updateProductQuantityInStock_whenAdjustedQuantityInvalid_shouldThrowBadRequestException() {
        Warehouse warehouse = Warehouse.builder().id(20L).name("Main Warehouse").build();
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(-10L).reservedQuantity(0L).warehouse(warehouse).build();
        List<StockQuantityVm> updates = List.of(new StockQuantityVm(1L, -5L, "invalid"));
        when(stockRepository.findAllById(eq(List.of(1L)))).thenReturn(List.of(stock));

        assertThrows(BadRequestException.class,
            () -> stockService.updateProductQuantityInStock(new StockQuantityUpdateVm(updates)));
    }
}