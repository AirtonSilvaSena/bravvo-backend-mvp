package br.com.bravvo.api.dto.common;

import java.util.List;

/**
 * DTO padrão para respostas paginadas.
 *
 * page = página atual (1-based) limit = quantidade por página total = total de
 * registros encontrados (sem paginação) pages = total de páginas
 * (ceil(total/limit)) items = lista de itens da página atual
 */
public class PagedResponseDTO<T> {

	private int page;
	private int limit;
	private long total;
	private int pages;
	private List<T> items;

	public PagedResponseDTO() {
	}

	public PagedResponseDTO(int page, int limit, long total, int pages, List<T> items) {
		this.page = page;
		this.limit = limit;
		this.total = total;
		this.pages = pages;
		this.items = items;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}
}
